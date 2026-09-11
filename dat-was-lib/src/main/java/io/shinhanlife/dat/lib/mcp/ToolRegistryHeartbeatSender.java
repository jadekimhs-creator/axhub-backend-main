package io.shinhanlife.dat.lib.mcp;


/**
 * @package io.shinhanlife.dat.mcc.service
 * @className ToolRegistryHeartbeatSender
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 * 
 * </pre>
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springaicommunity.mcp.annotation.McpTool;
import io.shinhanlife.dat.lib.annotation.GrowToolHint;
import io.shinhanlife.dat.lib.config.McpProperties;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import io.shinhanlife.dat.lib.util.ToolSchemaResolver;
import io.shinhanlife.dat.lib.metadata.ToolDefinition;
import io.shinhanlife.dat.lib.metadata.ToolDefinitionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

@Slf4j
@Component
@Configuration
@EnableScheduling
@ConditionalOnBean(McpToolExecutionService.class)
public class ToolRegistryHeartbeatSender {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final McpProperties mcpProperties;
    private final RestClient restClient = RestClient.create();
    private final ToolSchemaResolver toolSchemaResolver;
    private final ToolDefinitionRepository toolDefinitionRepository;

    @Autowired
    public ToolRegistryHeartbeatSender(ApplicationContext applicationContext, ObjectMapper objectMapper,
                                       McpProperties mcpProperties, ToolSchemaResolver toolSchemaResolver,
                                       @Nullable ToolDefinitionRepository toolDefinitionRepository) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
        this.mcpProperties = mcpProperties;
        this.toolSchemaResolver = toolSchemaResolver;
        this.toolDefinitionRepository = toolDefinitionRepository;
    }

    public ToolRegistryHeartbeatSender(ApplicationContext applicationContext, ObjectMapper objectMapper,
                                       McpProperties mcpProperties, ToolSchemaResolver toolSchemaResolver) {
        this(applicationContext, objectMapper, mcpProperties, toolSchemaResolver, null);
    }

    @Value("${axhub.gateway.url:http://localhost:8081}")
    private String gatewayUrl;

    @Value("${axhub.tool.url:http://localhost:8080}")
    private String podUrl;

    @Value("${spring.application.name:}")
    private String applicationName;

    private List<ToolMetadata> registeredTools = new ArrayList<>();
    
    @Getter
    private List<ToolMetadata> allScannedTools = new ArrayList<>();

    @PostConstruct
    public void init() {
        log.info(" [HeartbeatSender] 초기화 시작. Gateway URL: {}, Pod URL: {}", gatewayUrl, podUrl);
        scanAndBuildMetadata();
    }

    private void scanAndBuildMetadata() {
        Map<String, Object> allBeans = applicationContext.getBeansOfType(Object.class);
        for (Object bean : allBeans.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            
            for (Method method : targetClass.getDeclaredMethods()) {
                McpTool functionAnnotation = AnnotationUtils.findAnnotation(method, McpTool.class);
                GrowToolHint hintAnnotation = AnnotationUtils.findAnnotation(method, GrowToolHint.class);
                
                if (functionAnnotation != null) {
                    String baseName = functionAnnotation.name();
                    String rawSubToolName = functionAnnotation.name();
                    String subToolName = mcpProperties.getNamespace() != null && !mcpProperties.getNamespace().isEmpty()
                            ? mcpProperties.getNamespace() + "_" + rawSubToolName
                            : rawSubToolName;
                    // @GrowToolHint인 Tool은 메타데이터 조회에는 남기되,
                    // Gateway 등록 및 heartbeat 전송 대상에서는 제외합니다.
                    // ToolHint가 없는 기존 Tool은 이전 동작과 동일하게 등록합니다.
                    boolean isRegister = hintAnnotation == null || hintAnnotation.register();
                    if (!isRegister) {
                        log.info(" [HeartbeatSender] '{}' Tool is excluded from Gateway registration because . (tool name: {})",
                                baseName, subToolName);
                    }

                    ToolMetadata meta = new ToolMetadata();
                    meta.setUid(UUID.nameUUIDFromBytes(subToolName.getBytes()).toString());
                    String displayName = functionAnnotation.title().isEmpty() ? functionAnnotation.name() : functionAnnotation.title();
                    meta.setDisplayName(displayName);
                    meta.setName(subToolName);
                    meta.setSemver("1.0.0");
                    meta.setTimeoutMillis(5000L);
                    meta.setEnabled(true);
                    meta.setDescription(functionAnnotation.description());
                    meta.setCategoryKey(hintAnnotation == null || hintAnnotation.categoryKey().isBlank()
                            ? "common" : hintAnnotation.categoryKey());
                    meta.setIntegrationType("REST");
                    meta.setMciServiceId(hintAnnotation != null ? hintAnnotation.mappingId() : "");
                    meta.setPodUrl(podUrl);
                    meta.setModuleName(applicationName);
                    meta.setEndpoint(podUrl.replaceAll("/+$", "") + "/mcp/" + subToolName);
                    
                    meta.setVisible(true);
                    meta.setIsRegistered(isRegister);
                    meta.setRequiresApproval(hintAnnotation != null && hintAnnotation.requiresApproval());
                    
                    // extract standard hints from @McpTool.annotations()
                    McpTool.McpAnnotations ann = functionAnnotation.annotations();
                    if (ann != null) {
                        meta.setReadOnlyHint(ann.readOnlyHint());
                        meta.setDestructiveHint(ann.destructiveHint());
                        meta.setIdempotentHint(ann.idempotentHint());
                        meta.setOpenWorldHint(ann.openWorldHint());
                    } else {
                        meta.setReadOnlyHint(false);
                        meta.setDestructiveHint(true);
                        meta.setIdempotentHint(false);
                        meta.setOpenWorldHint(true);
                    }
                    
                    Map<String, String> prompts = new HashMap<>();
                    meta.setActionPrompts(prompts);
                    
                    if (method.getParameterCount() > 0) {
                        try {
                            Class<?> paramType = method.getParameterTypes()[0];
                            // TODO: ToolSchemaResolver may need to be updated to take McpTool instead of McpFunction
                            Map<String, Object> finalSchema = toolSchemaResolver.resolve(functionAnnotation, hintAnnotation, paramType);
                            meta.setParametersSchema(finalSchema);
                            Map<String, Object> outputSchema = toolSchemaResolver.resolveOutput(
                                    functionAnnotation, method.getReturnType(), hintAnnotation);
                            if (!outputSchema.isEmpty()) {
                                meta.setOutputSchema(outputSchema);
                            }
                        } catch (Exception e) {
                            log.error("Failed to generate schema for {}", subToolName, e);
                        }
                    }

                    enrichWithDefinition(meta, rawSubToolName, hintAnnotation);

                    if (isRegister) {
                        registeredTools.add(meta);
                    }
                    allScannedTools.add(meta);
                    log.info(" [HeartbeatSender] 도구 메타데이터 생성: {} (isRegistered: {})", meta.getUid(), isRegister);
                }
            }
        }
    }

    private void enrichWithDefinition(ToolMetadata meta, String rawToolName, GrowToolHint hintAnnotation) {
        if (toolDefinitionRepository == null) {
            return;
        }
        toolDefinitionRepository.findByName(rawToolName)
                .ifPresent(definition -> applyDefinition(meta, definition, hintAnnotation));
    }

    private void applyDefinition(ToolMetadata meta, ToolDefinition definition, GrowToolHint hintAnnotation) {
        meta.setDisplayName(definition.displayName());
        meta.setSemver(definition.version());
        meta.setCategoryKey(definition.categoryKey());
        meta.setFunctionDescription(definition.description().function());
        meta.setWhenToUse(definition.description().whenToUse());
        meta.setWhenNotToUse(definition.description().whenNotToUse());
        meta.setIoLimits(definition.description().ioLimits());
        meta.setDescription(definition.description().function());
        meta.setDisplayDescription(definition.displayDescription());
        meta.setExampleQueries(definition.exampleQueries());
        meta.setReadOnlyHint(definition.readOnly());
        meta.setDestructiveHint(definition.destructive());
        meta.setIdempotentHint(definition.idempotent());
        boolean explicitInputResource = hintAnnotation != null && !hintAnnotation.inputSchemaResource().isBlank();
        boolean explicitOutputResource = hintAnnotation != null && !hintAnnotation.outputSchemaResource().isBlank();
        if (!explicitInputResource) {
            meta.setParametersSchema(definition.parametersSchema());
        }
        if (!explicitOutputResource && definition.outputSchema() != null && !definition.outputSchema().isEmpty()) {
            meta.setOutputSchema(definition.outputSchema());
        }
        meta.setTags(definition.tags());
        meta.setMciServiceId(definition.legacyInterfaceId());
        meta.setRequiredEnvKeys(definition.requiredEnvKeys());
        meta.setOwnerOrg(definition.ownerOrg());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerAllTools() {
        if (registeredTools.isEmpty()) return;

        new Thread(() -> {
            for (ToolMetadata tool : registeredTools) {
                registerTool(tool);
            }
        }, "McpToolRegistrationThread").start();
    }

    @PreDestroy
    public void deregisterAllTools() {
        if (registeredTools.isEmpty()) return;

        for (ToolMetadata tool : registeredTools) {
            try {
                restClient.post()
                        .uri(gatewayUrl + "/mcp/api/v1/registry/deregister")
                        .header("Content-Type", "application/json")
                        .body(tool.getUid())
                        .retrieve()
                        .toBodilessEntity();
                log.info(" [HeartbeatSender] 툴 삭제(Deregister) 성공: {}", tool.getUid());
            } catch (Exception ex) {
                log.warn(" [HeartbeatSender] 툴 삭제 실패: {}", ex.getMessage());
            }
        }
    }

    private void registerTool(ToolMetadata tool) {
        int maxRetries = 12;
        int delayMs = 10000;
        for (int i = 0; i < maxRetries; i++) {
            try {
                restClient.post()
                        .uri(gatewayUrl + "/mcp/api/v1/registry/register")
                        .header("Content-Type", "application/json")
                        .body(tool)
                        .retrieve()
                        .toBodilessEntity();
                log.info(" [HeartbeatSender] 툴 등록 성공: {}", tool.getUid());
                return;
            } catch (Exception ex) {
                if (i == maxRetries - 1) {
                    log.error(" [HeartbeatSender] 툴 등록 최종 실패 ({}회 재시도): {}", maxRetries, ex.getMessage());
                } else {
                    log.warn(" [HeartbeatSender] 툴 등록 실패, {}초 후 재시도... ({}/{}): {}", delayMs/1000, i+1, maxRetries, ex.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
}
