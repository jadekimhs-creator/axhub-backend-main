package io.shinhanlife.axhub.biz.mcp.gateway.service;

import io.shinhanlife.axhub.biz.mcp.adapter.dto.Params;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import io.shinhanlife.axhub.biz.mcp.gateway.router.RequestValidator;
import io.shinhanlife.axhub.biz.mcp.gateway.router.ToolRouter;
import io.shinhanlife.axhub.biz.mcp.gateway.router.ResponseAggregator;
import io.shinhanlife.axhub.biz.mcp.gateway.client.ToolClient;
import java.util.Map;
import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteService {

    // 파이프라인의 핵심 컴포넌트들
    private final RequestValidator validator;
    private final ToolPlanner planner;
    private final ToolRouter router;
    private final ToolClient client;
    private final ResponseAggregator aggregator;
    private final KillSwitchService killSwitchService;

    public Object execute(Map<String, Object> payload, String tenantId) {
        log.info(" [ExecuteService] 전체 실행 흐름 제어 시작");

        // [비상 차단 1단계] Agent 단위 차단 검사
        killSwitchService.checkAgent(tenantId);

        // [비상 차단 2단계] Tool 단위 차단 검사
        String toolName = (String) payload.get("toolName");
        killSwitchService.checkTool(toolName);

        // 1. 검증 (Validator)
        validator.validate(payload);

        // 2. 실행 계획 생성 및 권한 검증 (Planner)
        var planObj = planner.createPlan(payload, tenantId);
        ToolMetadata plan = (ToolMetadata) planObj;

        // [비상 차단 3단계] Route/Legacy 단위 차단 검사
        if (plan.getIntegrationType() != null) {
            killSwitchService.checkRoute(plan.getIntegrationType());
        }

        // 3. 라우팅 (Router) - Redis의 Tool Registry 참조
        var targetPod = router.route(plan);

        // 4. 실제 Tool 호출 (Client) - Retry/Timeout 포함 (동적 제어 적용)
        var rawResponse = client.call(targetPod, payload, plan);

        // 5. 응답 정규화 및 병합 (Aggregator)
        return aggregator.normalize(rawResponse);
    }
}