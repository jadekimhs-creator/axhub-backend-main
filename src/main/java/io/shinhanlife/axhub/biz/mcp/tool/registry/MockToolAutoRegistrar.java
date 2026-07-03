package io.shinhanlife.axhub.biz.mcp.tool.registry;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.model.ToolConfig;
import io.shinhanlife.axhub.biz.mcp.tool.model.ToolFunction;
import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.axhub.biz.mcp.tool.util.JsonSchemaGenerator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MockToolAutoRegistrar {

    private final RestClient restClient = RestClient.builder().build();
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    @Value("${mcp.gateway.url:http://localhost:8081/mcp/api/v1}")
    private String gatewayUrl; 
    private final String API_KEY = "SHINHAN_MCP_TEST_KEY_9999";

    @Value("${server.port:8081}")
    private int serverPort;

    @Value("${mcp.tool.target:all}")
    private String toolTarget;

    @Value("${mcp.tool.host:localhost}")
    private String toolHost;

    private List<ToolConfig> TOOLS = new ArrayList<>();

    public MockToolAutoRegistrar(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (serverPort == 8081) return; // Gateway skip
        
        log.info(" [AutoDiscovery] @McpTool 어노테이션 스캔 시작...");
        Map<String, Object> toolBeans = applicationContext.getBeansWithAnnotation(McpTool.class);
        
        for (Object bean : toolBeans.values()) {
            McpTool mcpTool = bean.getClass().getAnnotation(McpTool.class);
            if (mcpTool == null) continue;

            // Target Group 필터링
            if (!"all".equals(toolTarget) && !mcpTool.name().equals(toolTarget) && !mcpTool.group().equals(toolTarget)) {
                continue;
            }

            List<ToolFunction> functions = new ArrayList<>();
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(McpFunction.class)) {
                    McpFunction mcpFunc = method.getAnnotation(McpFunction.class);
                    String finalSchema = mcpFunc.parameterSchema();
                    if (method.getParameterCount() > 0) {
                        Class<?> paramType = method.getParameterTypes()[0];
                        if (!Map.class.isAssignableFrom(paramType)) {
                            try {
                                Map<String, Object> autoSchema = JsonSchemaGenerator.generatePropertiesSchema(paramType);
                                finalSchema = objectMapper.writeValueAsString(autoSchema);
                            } catch (Exception e) {
                                log.error("Failed to generate schema for {}", paramType.getSimpleName(), e);
                            }
                        }
                    }
                    functions.add(new ToolFunction(mcpFunc.name(), mcpFunc.description(), mcpFunc.mappingId(), finalSchema));
                }
            }

            ToolConfig config = new ToolConfig(
                mcpTool.name(),
                mcpTool.description(),
                "/api/tool/execute/" + mcpTool.name(),
                mcpTool.group(),
                mcpTool.routingType(),
                functions
            );
            TOOLS.add(config);
            log.info(" [AutoDiscovery] 툴 발견: {} (함수 {}개)", mcpTool.name(), functions.size());
        }
    }

    public List<ToolConfig> getTools() {
        return TOOLS;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", API_KEY);
        return headers;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerOnStartup() {
        if (serverPort == 8081) return;
        
        log.info("[MockTool] Tool Pod 기동 완료! (Port: {})", serverPort);
        
        for (Object bean : applicationContext.getBeansWithAnnotation(McpTool.class).values()) {
            McpTool mcpTool = bean.getClass().getAnnotation(McpTool.class);
            if (!"all".equals(toolTarget) && !mcpTool.name().equals(toolTarget) && !mcpTool.group().equals(toolTarget)) {
                continue;
            }
            
            ToolMetadata myMetadata = new ToolMetadata();
            myMetadata.setToolName(mcpTool.name());
            myMetadata.setDescription(mcpTool.description());
            myMetadata.setDomainGroup(mcpTool.group());
            myMetadata.setEndpoint("/api/tool/execute/" + mcpTool.name());
            myMetadata.setPodUrl("http://" + toolHost + ":" + serverPort);
            myMetadata.setIntegrationType(mcpTool.routingType());
            myMetadata.setFailureRateThreshold(50);
            myMetadata.setSlidingWindowSize(20);
            myMetadata.setRateLimitForPeriod(100);

            Map<String, String> actionPrompts = new HashMap<>();
            List<String> actions = new ArrayList<>();
            List<String> actionDescs = new ArrayList<>();
            Map<String, Object> aggregatedProperties = new HashMap<>();

            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(McpFunction.class)) {
                    McpFunction mcpFunc = method.getAnnotation(McpFunction.class);
                    actions.add(mcpFunc.name());
                    actionDescs.add(mcpFunc.name() + "(" + mcpFunc.description() + ")");
                    actionPrompts.put(mcpFunc.name(), mcpFunc.prompt());
                    
                    if (method.getParameterCount() > 0) {
                        Class<?> paramType = method.getParameterTypes()[0];
                        if (!Map.class.isAssignableFrom(paramType)) {
                            // DTO 기반 자동 스키마 생성
                            Map<String, Object> autoSchema = JsonSchemaGenerator.generatePropertiesSchema(paramType);
                            aggregatedProperties.putAll(autoSchema);
                        } else {
                            // 기존 하드코딩 스키마 사용
                            String schemaJson = mcpFunc.parameterSchema();
                            if (!"{}".equals(schemaJson)) {
                                try {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> schemaMap = objectMapper.readValue(schemaJson, Map.class);
                                    aggregatedProperties.putAll(schemaMap);
                                } catch (Exception e) {
                                    log.error(" [MockTool] Failed to parse parameterSchema for {}: {}", mcpFunc.name(), e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            
            myMetadata.setActionPrompts(actionPrompts);

            Map<String, Object> parametersSchema = new HashMap<>();
            parametersSchema.put("type", "object");
            Map<String, Object> properties = new HashMap<>();
            properties.putAll(aggregatedProperties);
            
            Map<String, Object> actionProperty = new HashMap<>();
            actionProperty.put("type", "string");
            actionProperty.put("enum", actions);
            actionProperty.put("description", "실행할 함수명: " + String.join(", ", actionDescs));
            
            properties.put("action", actionProperty);
            parametersSchema.put("properties", properties);
            parametersSchema.put("required", Arrays.asList("action"));
            
            myMetadata.setParametersSchema(parametersSchema);

            try {
                String registerUrl = gatewayUrl + "/registry/register";
                restClient.post()
                         .uri(registerUrl)
                         .headers(h -> h.addAll(createHeaders()))
                         .body(myMetadata)
                         .retrieve()
                         .body(String.class);
                log.info(" [MockTool] {} 자동 등록 성공! (Pod URL: {})", mcpTool.name(), myMetadata.getPodUrl());
            } catch (Exception e) {
                log.error(" [MockTool] {} 자동 등록 실패: {}", mcpTool.name(), e.getMessage());
            }
        }
        log.info("");
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        if (serverPort == 8081) return;

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        
        for (ToolConfig tool : TOOLS) {
            try {
                String heartbeatUrl = gatewayUrl + "/registry/heartbeat";
                restClient.post()
                         .uri(heartbeatUrl)
                         .headers(h -> h.addAll(headers))
                         .body(tool.getName())
                         .retrieve()
                         .body(String.class);
                log.info(" [MockTool] {} Heartbeat 서버 전송 완료", tool.getName());
            } catch (Exception e) {
                log.error(" [MockTool] {} Heartbeat 전송 실패: {}", tool.getName(), e.getMessage());
            }
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void deregisterOnShutdown() {
        if (serverPort == 8081) return;

        log.info("\n [MockTool] Tool Pod (Port: {}) 종료 중... Gateway에 툴 해제를 요청합니다.", serverPort);
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        
        for (ToolConfig tool : TOOLS) {
            try {
                String deregisterUrl = gatewayUrl + "/registry/deregister";
                restClient.post()
                         .uri(deregisterUrl)
                         .headers(h -> h.addAll(headers))
                         .body(tool.getName())
                         .retrieve()
                         .body(String.class);
                log.info(" [MockTool] {} 명시적 해제 성공!", tool.getName());
            } catch (Exception e) {
                log.error(" [MockTool] {} 명시적 해제 실패: {}", tool.getName(), e.getMessage());
            }
        }
    }
}
