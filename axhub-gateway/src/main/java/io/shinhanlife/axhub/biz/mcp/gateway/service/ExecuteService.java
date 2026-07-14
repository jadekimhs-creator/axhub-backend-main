package io.shinhanlife.axhub.biz.mcp.gateway.service;

import io.shinhanlife.axhub.biz.mcp.adapter.dto.Params;
import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteService {

    private final ToolPlanner planner;
    private final KillSwitchService killSwitchService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final io.shinhanlife.axhub.biz.mcp.gateway.guardrail.SensitiveDataMasker dataMasker;
    private final io.shinhanlife.axhub.biz.mcp.gateway.guardrail.GuardrailService guardrailService;
    private final io.shinhanlife.axhub.biz.mcp.gateway.security.McpRequestContextResolver contextResolver;
    private final io.shinhanlife.axhub.biz.mcp.gateway.audit.AuditLogService auditLogService;
    private final io.shinhanlife.axhub.biz.mcp.gateway.resilience.CircuitBreakerService circuitBreakerService;
    private final RestClient restClient = RestClient.create();

    public Object execute(Map<String, Object> payload, String tenantId) {
        log.info(" [ExecuteService] 전체 실행 흐름 제어 시작");

        killSwitchService.checkAgent(tenantId);

        Map<String, Object> params = payload.containsKey("params") ? (Map<String, Object>) payload.get("params") : null;
        String toolName = params != null ? (String) params.get("name") : (String) payload.get("toolName");
        killSwitchService.checkTool(toolName);

        var planObj = planner.createPlan(payload, tenantId);
        ToolMetadata plan = (ToolMetadata) planObj;

        io.shinhanlife.axhub.biz.mcp.gateway.security.McpRequestContext context = contextResolver.current();

        // --- Guardrail 검증 (Admin UI 및 AI 요청 모두 적용) ---
        com.fasterxml.jackson.databind.node.ObjectNode argumentsNode = objectMapper.createObjectNode();
        if (params != null && params.containsKey("arguments")) {
            argumentsNode = objectMapper.valueToTree(params.get("arguments"));
            guardrailService.validate(plan, argumentsNode);
        }

        auditLogService.toolStarted(context, toolName, argumentsNode);

        if (plan.getIntegrationType() != null) {
            killSwitchService.checkRoute(plan.getIntegrationType());
        }

        io.shinhanlife.axhub.biz.mcp.gateway.resilience.CircuitBreaker circuitBreaker = circuitBreakerService.breaker("tool:" + toolName, 0, 0);
        circuitBreaker.beforeCall();

        // Dynamic Routing: Find the target Pod from the Redis registry
        String targetUrl = "http://localhost:8084"; // Fallback (tool-other)
        if (plan.getPodUrl() != null && !plan.getPodUrl().isEmpty()) {
            targetUrl = plan.getPodUrl();
        }

        log.info(" [ExecuteService] 라우팅 목적지: {}", targetUrl);
        String executeApiUrl = targetUrl + "/mcp/api/v1/tools/call";

        long startTime = System.currentTimeMillis();
        boolean success = false;
        String errorCode = null;

        // Forward the request to the target tool pod using RestClient with fallback
        try {
            try {
                log.info(" [ExecuteService] 요청 페이로드(마스킹 적용): {}", objectMapper.writeValueAsString(dataMasker.mask(objectMapper.valueToTree(payload))));
            } catch (Exception ignore) {}
            
            Object result = executeWithUrl(payload, executeApiUrl);
            circuitBreaker.recordSuccess();
            success = true;
            return result;
        } catch (Exception e) {
            errorCode = io.shinhanlife.axhub.biz.mcp.gateway.resilience.FailureType.SERVER_ERROR.name();
            circuitBreaker.recordFailure(io.shinhanlife.axhub.biz.mcp.gateway.resilience.FailureType.SERVER_ERROR);
            if (executeApiUrl.contains("http://tool-")) {
                String fallbackUrl = executeApiUrl.replaceAll("http://tool-[a-zA-Z0-9-]+", "http://localhost");
                log.warn(" [ExecuteService] 호스트를 찾을 수 없어 localhost로 재시도합니다: {}", fallbackUrl);
                try {
                    Object result = executeWithUrl(payload, fallbackUrl);
                    circuitBreaker.recordSuccess();
                    success = true;
                    errorCode = null;
                    return result;
                } catch (Exception ex) {
                    circuitBreaker.recordFailure(io.shinhanlife.axhub.biz.mcp.gateway.resilience.FailureType.SERVER_ERROR);
                    log.error(" [ExecuteService] localhost 재시도 실패: {}", ex.getMessage());
                    throw new io.shinhanlife.axhub.biz.mcp.gateway.resilience.ToolExecutionException(io.shinhanlife.axhub.biz.mcp.gateway.resilience.FailureType.SERVER_ERROR, "Tool Pod 호출 실패 (localhost 재시도 포함): " + ex.getMessage());
                }
            }
            log.error(" [ExecuteService] Tool Pod 호출 실패: {}", e.getMessage());
            throw new io.shinhanlife.axhub.biz.mcp.gateway.resilience.ToolExecutionException(io.shinhanlife.axhub.biz.mcp.gateway.resilience.FailureType.SERVER_ERROR, "Tool Pod 호출 실패: " + e.getMessage());
        } finally {
            long elapsedMillis = System.currentTimeMillis() - startTime;
            auditLogService.toolFinished(context, toolName, elapsedMillis, success, errorCode);
        }
    }

    private Object executeWithUrl(Map<String, Object> payload, String url) {
        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                 // TODO: Use actual tenant's key
                .header("X-Trace-Id", UUID.randomUUID().toString())
                .body(payload)
                .retrieve()
                .body(Object.class);
    }
}