package io.shinhanlife.axhub.biz.mcp.tool.registry;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
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

    private List<String> registeredTools = new ArrayList<>();

    public MockToolAutoRegistrar(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
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
        registeredTools.clear();
        
        for (Object bean : applicationContext.getBeansWithAnnotation(McpTool.class).values()) {
            McpTool mcpTool = bean.getClass().getAnnotation(McpTool.class);
            if (!"all".equals(toolTarget) && !mcpTool.name().equals(toolTarget) && !mcpTool.group().equals(toolTarget)) {
                continue;
            }
            
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(McpFunction.class)) {
                    McpFunction mcpFunc = method.getAnnotation(McpFunction.class);
                    
                    ToolMetadata myMetadata = new ToolMetadata();
                    myMetadata.setToolName(mcpFunc.name()); // Function -> Tool로 승격
                    myMetadata.setDescription(mcpFunc.description());
                    myMetadata.setDomainGroup(mcpTool.group());
                    myMetadata.setEndpoint("/api/tool/execute/" + mcpFunc.name());
                    myMetadata.setPodUrl("http://" + toolHost + ":" + serverPort);
                    myMetadata.setIntegrationType(mcpTool.routingType());
                    myMetadata.setFailureRateThreshold(50);
                    myMetadata.setSlidingWindowSize(20);
                    myMetadata.setRateLimitForPeriod(100);

                    Map<String, String> actionPrompts = new HashMap<>();
                    actionPrompts.put(mcpFunc.name(), mcpFunc.prompt());
                    myMetadata.setActionPrompts(actionPrompts);

                    Map<String, Object> parametersSchema = new HashMap<>();
                    parametersSchema.put("type", "object");
                    Map<String, Object> aggregatedProperties = new HashMap<>();
                    
                    if (method.getParameterCount() > 0) {
                        Class<?> paramType = method.getParameterTypes()[0];
                        if (!Map.class.isAssignableFrom(paramType)) {
                            // DTO 기반 스키마 (비즈니스 파라미터만)
                            Map<String, Object> autoSchema = JsonSchemaGenerator.generatePropertiesSchema(paramType);
                            aggregatedProperties.putAll(autoSchema);
                        } else {
                            String schemaJson = mcpFunc.parameterSchema();
                            if (!"{}".equals(schemaJson)) {
                                try {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> schemaMap = objectMapper.readValue(schemaJson, Map.class);
                                    aggregatedProperties.putAll(schemaMap);
                                } catch (Exception e) {
                                    log.error(" [MockTool] 스키마 파싱 실패 {}: {}", mcpFunc.name(), e.getMessage());
                                }
                            }
                        }
                    }
                    
                    parametersSchema.put("properties", aggregatedProperties);
                    myMetadata.setParametersSchema(parametersSchema);

                    try {
                        String registerUrl = gatewayUrl + "/registry/register";
                        restClient.post()
                                 .uri(registerUrl)
                                 .headers(h -> h.addAll(createHeaders()))
                                 .body(myMetadata)
                                 .retrieve()
                                 .body(String.class);
                        log.info(" [MockTool] 함수 툴 승격 및 등록 성공: {} (Pod: {})", mcpFunc.name(), myMetadata.getPodUrl());
                        registeredTools.add(mcpFunc.name());
                    } catch (Exception e) {
                        log.error(" [MockTool] {} 등록 실패: {}", mcpFunc.name(), e.getMessage());
                    }
                }
            }
        }
        log.info("");
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        if (serverPort == 8081) return;

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        
        boolean needsReRegistration = false;
        for (String toolName : registeredTools) {
            try {
                String heartbeatUrl = gatewayUrl + "/registry/heartbeat";
                restClient.post()
                         .uri(heartbeatUrl)
                         .headers(h -> h.addAll(headers))
                         .body(toolName)
                         .retrieve()
                         .body(String.class);
                log.info(" [MockTool] {} Heartbeat 전송 완료", toolName);
            } catch (Exception e) {
                log.error(" [MockTool] {} Heartbeat 전송 실패: {}", toolName, e.getMessage());
                needsReRegistration = true;
            }
        }
        
        if (needsReRegistration) {
            log.info(" [MockTool] 하트비트 실패로 재등록을 시도합니다.");
            registerOnStartup();
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void deregisterOnShutdown() {
        if (serverPort == 8081) return;

        log.info("\n [MockTool] Tool Pod (Port: {}) 종료 중...", serverPort);
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        
        for (String toolName : registeredTools) {
            try {
                String deregisterUrl = gatewayUrl + "/registry/deregister";
                restClient.post()
                         .uri(deregisterUrl)
                         .headers(h -> h.addAll(headers))
                         .body(toolName)
                         .retrieve()
                         .body(String.class);
                log.info(" [MockTool] {} 명시적 해제 성공!", toolName);
            } catch (Exception e) {
                log.error(" [MockTool] {} 해제 실패: {}", toolName, e.getMessage());
            }
        }
    }
}
