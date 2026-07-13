package io.shinhanlife.axhub.biz.mcp.tool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.config.McpProperties;
import io.shinhanlife.axhub.biz.mcp.tool.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.tool.util.JsonSchemaGenerator;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ToolRegistryHeartbeatSender {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final McpProperties mcpProperties;
    private final RestClient restClient = RestClient.create();

    @Value("${axhub.gateway.url:http://localhost:8081}")
    private String gatewayUrl;

    @Value("${axhub.tool.url:http://localhost:8080}")
    private String podUrl;

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
            McpTool toolAnnotation = AnnotationUtils.findAnnotation(targetClass, McpTool.class);
            
            for (Method method : targetClass.getDeclaredMethods()) {
                McpFunction functionAnnotation = AnnotationUtils.findAnnotation(method, McpFunction.class);
                if (functionAnnotation != null) {
                    String baseName = functionAnnotation.displayName();
                    String rawSubToolName = functionAnnotation.name();
                    String subToolName = mcpProperties.getNamespace() != null && !mcpProperties.getNamespace().isEmpty()
                            ? mcpProperties.getNamespace() + "_" + rawSubToolName
                            : rawSubToolName;

                    boolean isRegister = functionAnnotation.register();
                    if (!isRegister) {
                        log.info(" [HeartbeatSender] '{}' 툴은 어노테이션 설정에 의해 외부 등록(Redis) 대상에서 제외되었습니다. (최종 이름: {})", baseName, subToolName);
                    }

                    ToolMetadata meta = new ToolMetadata();
                    meta.setUid(UUID.nameUUIDFromBytes(subToolName.getBytes()).toString());
                    meta.setDisplayName(baseName);
                    meta.setName(subToolName);
                    meta.setSemver("1.0.0");
                    meta.setDescription(functionAnnotation.description());
                    meta.setCategoryKey(toolAnnotation.categoryKey());
                    meta.setIntegrationType(toolAnnotation.routingType());
                    meta.setMciServiceId(functionAnnotation.mappingId());
                    meta.setPodUrl(podUrl);
                    
                    boolean isVisible = functionAnnotation.visible();
                    meta.setVisible(isVisible);
                    meta.setIsRegistered(isRegister);
                    meta.setRequiresApproval(functionAnnotation.requiresApproval());
                    
                    Map<String, String> prompts = new HashMap<>();
                    String promptText = functionAnnotation.prompt();
                    prompts.put(subToolName, promptText);
                    meta.setActionPrompts(prompts);
                    
                    if (method.getParameterCount() > 0) {
                        try {
                            Class<?> paramType = method.getParameterTypes()[0];
                            Map<String, Object> schema = JsonSchemaGenerator.generatePropertiesSchema(paramType);
                            Map<String, Object> finalSchema = new HashMap<>();
                            finalSchema.put("type", "object");
                            finalSchema.put("properties", schema);
                            meta.setParametersSchema(finalSchema);
                        } catch (Exception e) {
                            log.error("Failed to generate schema for {}", subToolName, e);
                        }
                    }

                    if (isRegister) {
                        registeredTools.add(meta);
                    }
                    allScannedTools.add(meta);
                    log.info(" [HeartbeatSender] 도구 메타데이터 생성: {} (isRegistered: {})", meta.getUid(), isRegister);
                }
            }
        }
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeats() {
        if (registeredTools.isEmpty()) return;

        for (ToolMetadata tool : registeredTools) {
            try {
                ResponseEntity<String> response = restClient.post()
                        .uri(gatewayUrl + "/mcp/api/v1/registry/heartbeat")
                        .header("Content-Type", "application/json")
                        
                        .body(tool.getUid())
                        .retrieve()
                        .toEntity(String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info(" [HeartbeatSender] 하트비트 전송 성공: {}", tool.getUid());
                }
            } catch (Exception e) {
                log.warn(" [HeartbeatSender] 하트비트 전송 실패 ({}): {}. 재등록을 시도합니다.", tool.getUid(), e.getMessage());
                registerTool(tool);
            }
        }
    }

    private void registerTool(ToolMetadata tool) {
        try {
            restClient.post()
                    .uri(gatewayUrl + "/mcp/api/v1/registry/register")
                    .header("Content-Type", "application/json")
                    
                    .body(tool)
                    .retrieve()
                    .toBodilessEntity();
            log.info(" [HeartbeatSender] 툴 재등록 성공: {}", tool.getUid());
        } catch (Exception ex) {
            log.error(" [HeartbeatSender] 툴 등록 실패: {}", ex.getMessage());
        }
    }
}