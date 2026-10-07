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
import io.shinhanlife.dat.lib.annotation.GrowToolHint;
import io.shinhanlife.dat.lib.config.McpProperties;
import io.shinhanlife.dat.lib.metadata.ToolDefinition;
import io.shinhanlife.dat.lib.metadata.ToolDefinitionRepository;
import io.shinhanlife.dat.lib.util.ToolSchemaResolver;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import jakarta.annotation.PostConstruct;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

@Slf4j
@Component
@ConditionalOnBean(McpToolExecutionService.class)
public class ToolRegistryHeartbeatSender {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final McpProperties mcpProperties;
    private final ToolSchemaResolver toolSchemaResolver;
    private final ToolDefinitionRepository toolDefinitionRepository;

    @Autowired
    public ToolRegistryHeartbeatSender(ApplicationContext applicationContext, ObjectMapper objectMapper, McpProperties mcpProperties, ToolSchemaResolver toolSchemaResolver, @Nullable ToolDefinitionRepository toolDefinitionRepository) {

        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
        this.mcpProperties = mcpProperties;
        this.toolSchemaResolver = toolSchemaResolver;
        this.toolDefinitionRepository = toolDefinitionRepository;
    }

    public ToolRegistryHeartbeatSender(ApplicationContext applicationContext, ObjectMapper objectMapper, McpProperties mcpProperties, ToolSchemaResolver toolSchemaResolver) {

        this(applicationContext, objectMapper, mcpProperties, toolSchemaResolver, null);
    }

    @Value("${axhub.tool.url:http://localhost:8080}")
    private String podUrl;

    @Value("${spring.application.name:}")
    private String applicationName;

    @Getter
    private List<ToolMetadata> allScannedTools = new ArrayList<>();

    @PostConstruct
    public void init() {
        log.info(" [ToolScanner] 초기화 시작. Pod URL: {}", podUrl);
        scanAndBuildMetadata();
    }

    private void scanAndBuildMetadata() {

        /*
         * 재호출될 가능성을 고려해서
         * 기존 Metadata 제거
         */
        allScannedTools.clear();
        Map<String, Object> allBeans = applicationContext.getBeansOfType(Object.class);
        for (Object bean : allBeans.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getDeclaredMethods()) {
                /*
                 * Annotation 검색
                 *
                 * 1순위 : Interface
                 * 2순위 : 구현체
                 */
                ToolAnnotationMetadata annotationMetadata = findToolAnnotationMetadata(targetClass, method);

                if (annotationMetadata == null) {
                    continue;
                }

                McpTool functionAnnotation = annotationMetadata.mcpTool();
                GrowToolHint hintAnnotation = annotationMetadata.growToolHint();

                String rawSubToolName = functionAnnotation.name();
                /*
                 * Namespace 적용
                 */
                String subToolName = mcpProperties.getNamespace() != null && !mcpProperties.getNamespace().isEmpty() ? mcpProperties.getNamespace() + "_" + rawSubToolName : rawSubToolName;

                /*
                 * register 값은 외부 Gateway 전송이 아니라
                 * Tool 메타데이터 호환 필드로 유지
                 */
                boolean isRegister = hintAnnotation == null || hintAnnotation.register();
                ToolMetadata meta = new ToolMetadata();
                meta.setUid(UUID.nameUUIDFromBytes(subToolName.getBytes()).toString());
                String displayName = functionAnnotation.title().isEmpty() ? functionAnnotation.name() : functionAnnotation.title();
                meta.setDisplayName(displayName);
                meta.setName(subToolName);
                meta.setSemver("1.0.0");
                meta.setTimeoutMillis(hintAnnotation == null ? 5000L : positiveOrDefault(hintAnnotation.timeoutMillis(), 5000L));
                meta.setRetryMaxAttempts(hintAnnotation == null ? 3 : positiveOrDefault(hintAnnotation.retryMaxAttempts(), 3));
                meta.setEnabled(true);
                meta.setDescription(functionAnnotation.description());

                meta.setCategoryKey(hintAnnotation == null || hintAnnotation.categoryKey().isBlank() ? "common" : hintAnnotation.categoryKey());
                meta.setIntegrationType("REST");
                meta.setMciServiceId(hintAnnotation != null ? hintAnnotation.mappingId() : "");

                meta.setPodUrl(podUrl);
                meta.setModuleName(applicationName);

                meta.setEndpoint("/mcp/" + subToolName);

                meta.setVisible(true);
                meta.setIsRegistered(isRegister);
                meta.setRequiresApproval(hintAnnotation != null && hintAnnotation.requiresApproval());

                /*
                 * MCP standard hint
                 */
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

                if (hintAnnotation != null) {
                    if (!hintAnnotation.displayDescription().isBlank()) meta.setDisplayDescription(hintAnnotation.displayDescription());
                    if (!hintAnnotation.functionDescription().isBlank()) meta.setFunctionDescription(hintAnnotation.functionDescription());
                    if (!hintAnnotation.whenToUse().isBlank()) meta.setWhenToUse(hintAnnotation.whenToUse());
                    if (!hintAnnotation.whenNotToUse().isBlank()) meta.setWhenNotToUse(hintAnnotation.whenNotToUse());
                    if (!hintAnnotation.ioLimits().isBlank()) meta.setIoLimits(hintAnnotation.ioLimits());
                    if (hintAnnotation.exampleQueries().length > 0) meta.setExampleQueries(java.util.List.of(hintAnnotation.exampleQueries()));
                    if (hintAnnotation.tags().length > 0) meta.setTags(java.util.List.of(hintAnnotation.tags()));
                    if (hintAnnotation.requiredEnvKeys().length > 0) meta.setRequiredEnvKeys(java.util.List.of(hintAnnotation.requiredEnvKeys()));
                    if (!hintAnnotation.ownerOrg().isBlank()) meta.setOwnerOrg(hintAnnotation.ownerOrg());
                }

                Map<String, String> prompts = new HashMap<>();

                meta.setActionPrompts(prompts);

                /*
                 * Request / Response Schema 생성
                 *
                 * Annotation은 Interface에서 읽어오지만
                 * 실제 Method의 Parameter/Return Type은
                 * 구현체 Method를 기준으로 사용
                 */
                if (method.getParameterCount() > 0) {

                    try {

                        Class<?> paramType = method.getParameterTypes()[0];
                        Map<String, Object> finalSchema = ToolMetadataMcpMapper.normalizeSchema(
                                toolSchemaResolver.resolve(functionAnnotation, hintAnnotation, paramType));
                        meta.setParametersSchema(finalSchema);
                        Map<String, Object> outputSchema = toolSchemaResolver.resolveOutput(functionAnnotation, method.getReturnType(), hintAnnotation);
                        if (!outputSchema.isEmpty()) {
                            meta.setOutputSchema(outputSchema);
                        }

                    } catch (Exception e) {

                        log.error("Failed to generate schema for {}", subToolName, e);
                    }
                }

                /*
                 * YML Tool Definition 병합
                 */
                enrichWithDefinition(meta, rawSubToolName, hintAnnotation);
                allScannedTools.add(meta);
                log.info(" [ToolScanner] 도구 메타데이터 생성: " + "name={}, class={}#{}, " + "isRegistered={}", subToolName, targetClass.getSimpleName(), method.getName(), isRegister);
            }
        }

        log.info(" [ToolScanner] 총 {}개 Tool 메타데이터 생성 완료", allScannedTools.size());
    }

    private long positiveOrDefault(long value, long defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    private int positiveOrDefault(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    /**
     * @McpTool / @GrowToolHint 검색
     * <p>
     * 우선순위
     * <p>
     * 1. Interface
     * 2. Implementation
     */
    private ToolAnnotationMetadata findToolAnnotationMetadata(Class<?> targetClass, Method method) {

        /*
         * ========================================
         * STEP 1.
         * Interface부터 검색
         * ========================================
         */
        for (Class<?> interfaceClass : ClassUtils.getAllInterfacesForClassAsSet(targetClass)) {

            Method interfaceMethod = ReflectionUtils.findMethod(interfaceClass, method.getName(), method.getParameterTypes());
            if (interfaceMethod == null) {
                continue;
            }

            McpTool mcpTool = AnnotationUtils.findAnnotation(interfaceMethod, McpTool.class);
            if (mcpTool == null) {
                continue;
            }
            GrowToolHint growToolHint = AnnotationUtils.findAnnotation(interfaceMethod, GrowToolHint.class);
            log.debug(" [ToolScanner] Interface @McpTool 발견: " + "{}#{} -> {}", interfaceClass.getSimpleName(), interfaceMethod.getName(), mcpTool.name());

            return new ToolAnnotationMetadata(mcpTool, growToolHint);
        }

        /*
         * ========================================
         * STEP 2.
         * Interface에서 찾지 못한 경우에만
         * 구현체 Method 검색
         * ========================================
         */
        McpTool mcpTool = AnnotationUtils.findAnnotation(method, McpTool.class);

        if (mcpTool == null) {
            return null;
        }

        GrowToolHint growToolHint = AnnotationUtils.findAnnotation(method, GrowToolHint.class);

        log.debug(" [ToolScanner] Implementation @McpTool 발견: " + "{}#{} -> {}", targetClass.getSimpleName(), method.getName(), mcpTool.name());

        return new ToolAnnotationMetadata(mcpTool, growToolHint);
    }

    private void enrichWithDefinition(ToolMetadata meta, String rawToolName, GrowToolHint hintAnnotation) {

        if (toolDefinitionRepository == null) {
            return;
        }

        toolDefinitionRepository.findByName(rawToolName).ifPresent(definition -> applyDefinition(meta, definition, hintAnnotation));
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
            meta.setParametersSchema(ToolMetadataMcpMapper.normalizeSchema(definition.parametersSchema()));
        }

        if (!explicitOutputResource && definition.outputSchema() != null && !definition.outputSchema().isEmpty()) {
            meta.setOutputSchema(definition.outputSchema());
        }

        meta.setTags(definition.tags());
        meta.setMciServiceId(definition.legacyInterfaceId());
        meta.setRequiredEnvKeys(definition.requiredEnvKeys());
        meta.setOwnerOrg(definition.ownerOrg());
    }

    /**
     * Annotation 검색 결과
     */
    private record ToolAnnotationMetadata(McpTool mcpTool, GrowToolHint growToolHint) {
    }
}
