package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.tool.util.JsonSchemaGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.shinhanlife.axhub.biz.mcp.tool.config.McpProperties;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @PostConstruct
    public void init() {
        log.info(" [HeartbeatSender] 초기화 시작. Gateway URL: {}, Pod URL: {}", gatewayUrl, podUrl);
        scanAndBuildMetadata();
    }

    private void scanAndBuildMetadata() {
        Map<String, Object> toolBeans = applicationContext.getBeansWithAnnotation(McpTool.class);
        for (Object bean : toolBeans.values()) {
            McpTool toolAnnotation = bean.getClass().getAnnotation(McpTool.class);
            
            for (Method method : bean.getClass().getDeclaredMethods()) {
                McpFunction functionAnnotation = method.getAnnotation(McpFunction.class);
                if (functionAnnotation != null) {
                    String baseName = functionAnnotation.name();
                    String finalName = mcpProperties.getNamespace() != null && !mcpProperties.getNamespace().isEmpty()
                            ? mcpProperties.getNamespace() + "_" + baseName
                            : baseName;

                    McpProperties.FunctionProp prop = null;
                    if (mcpProperties.getFunctions() != null) {
                        prop = mcpProperties.getFunctions().get(baseName);
                    }

                    boolean isRegister = prop != null && prop.getRegister() != null ? prop.getRegister() : functionAnnotation.register();
                    if (!isRegister) {
                        log.info(" [HeartbeatSender] '{}' 툴은 설정에 의해 외부 등록(Redis) 대상에서 제외되었습니다. (최종 이름: {})", baseName, finalName);
                        continue;
                    }

                    ToolMetadata meta = new ToolMetadata();
                    meta.setToolName(finalName);
                    meta.setDescription(prop != null && prop.getDescription() != null ? prop.getDescription() : functionAnnotation.description());
                    meta.setDomainGroup(toolAnnotation.group());
                    meta.setIntegrationType(toolAnnotation.routingType());
                    meta.setMciServiceId(prop != null && prop.getMappingId() != null ? prop.getMappingId() : functionAnnotation.mappingId());
                    meta.setPodUrl(podUrl);
                    
                    boolean isVisible = prop != null && prop.getVisible() != null ? prop.getVisible() : functionAnnotation.visible();
                    meta.setVisible(isVisible);
                    
                    Map<String, String> prompts = new HashMap<>();
                    String promptText = prop != null && prop.getPrompt() != null ? prop.getPrompt() : functionAnnotation.prompt();
                    prompts.put(finalName, promptText);
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
                            log.error("Failed to generate schema for {}", finalName, e);
                        }
                    }

                    registeredTools.add(meta);
                    log.info(" [HeartbeatSender] 도구 메타데이터 생성: {}", meta.getToolName());
                }
            }
        }
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeats() {
        if (registeredTools.isEmpty()) return;

        for (ToolMetadata tool : registeredTools) {
            try {
                org.springframework.http.ResponseEntity<String> response = restClient.post()
                        .uri(gatewayUrl + "/mcp/api/v1/registry/heartbeat")
                        .header("Content-Type", "application/json")
                        .header("X-API-KEY", "SHINHAN_MCP_TEST_KEY_9999")
                        .body(tool.getToolName())
                        .retrieve()
                        .toEntity(String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info(" [HeartbeatSender] 하트비트 전송 성공: {}", tool.getToolName());
                }
            } catch (Exception e) {
                log.warn(" [HeartbeatSender] 하트비트 전송 실패 ({}): {}. 재등록을 시도합니다.", tool.getToolName(), e.getMessage());
                registerTool(tool);
            }
        }
    }

    private void registerTool(ToolMetadata tool) {
        try {
            restClient.post()
                    .uri(gatewayUrl + "/mcp/api/v1/registry/register")
                    .header("Content-Type", "application/json")
                    .header("X-API-KEY", "SHINHAN_MCP_TEST_KEY_9999")
                    .body(tool)
                    .retrieve()
                    .toBodilessEntity();
            log.info(" [HeartbeatSender] 툴 재등록 성공: {}", tool.getToolName());
        } catch (Exception ex) {
            log.error(" [HeartbeatSender] 툴 등록 실패: {}", ex.getMessage());
        }
    }
}
