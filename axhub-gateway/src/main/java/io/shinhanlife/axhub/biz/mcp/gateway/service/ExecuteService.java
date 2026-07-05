package io.shinhanlife.axhub.biz.mcp.gateway.service;

import io.shinhanlife.axhub.biz.mcp.adapter.dto.Params;
import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteService {

    private final ToolPlanner planner;
    private final KillSwitchService killSwitchService;
    private final RestTemplate restTemplate = new RestTemplate();

    public Object execute(Map<String, Object> payload, String tenantId) {
        log.info(" [ExecuteService] 전체 실행 흐름 제어 시작");

        killSwitchService.checkAgent(tenantId);

        Map<String, Object> params = payload.containsKey("params") ? (Map<String, Object>) payload.get("params") : null;
        String toolName = params != null ? (String) params.get("name") : (String) payload.get("toolName");
        killSwitchService.checkTool(toolName);

        var planObj = planner.createPlan(payload, tenantId);
        ToolMetadata plan = (ToolMetadata) planObj;

        if (plan.getIntegrationType() != null) {
            killSwitchService.checkRoute(plan.getIntegrationType());
        }

        // Dynamic Routing: Find the target Pod from the Redis registry
        String targetUrl = "http://localhost:8082"; // Fallback
        if (plan.getPodUrl() != null && !plan.getPodUrl().isEmpty()) {
            targetUrl = plan.getPodUrl();
        }

        log.info(" [ExecuteService] 라우팅 목적지: {}", targetUrl);
        String executeApiUrl = targetUrl + "/mcp/api/v1/tools/call";

        // Forward the request to the target tool pod
        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(executeApiUrl, payload, Object.class);
            return response.getBody();
        } catch (Exception e) {
            log.error(" [ExecuteService] Tool Pod 호출 실패: {}", e.getMessage());
            throw new RuntimeException("Tool Pod 호출 실패: " + e.getMessage());
        }
    }
}