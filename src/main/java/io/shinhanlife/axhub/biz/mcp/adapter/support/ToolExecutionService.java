package io.shinhanlife.axhub.biz.mcp.adapter.support;

import io.shinhanlife.axhub.biz.mcp.adapter.connector.LegacyEimsConnector;
import io.shinhanlife.axhub.common.mcp.adapter.dto.ErrorDetail;
import io.shinhanlife.axhub.common.mcp.adapter.dto.JsonRpcRequest;
import io.shinhanlife.axhub.common.mcp.adapter.dto.JsonRpcResponse;
import io.shinhanlife.axhub.common.mcp.adapter.dto.Params;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolExecutionService {

    //  방어막(서킷/캐시)이 적용된 커넥터 주입
    private final LegacyEimsConnector legacyEimsConnector;

    /**
     * AI Agent가 호출한 툴을 실제 레거시 커넥터로 전달합니다.
     */
    public JsonRpcResponse executeTool(JsonRpcRequest request) {
        Params params = request.getParams();

        // 1. 필수 파라미터 검증
        if (params == null || params.getName() == null) {
            return createErrorResponse(request.getId(), -32602, "Invalid params: 'name' is required");
        }

        try {
            // 2. Connector의 executeByTool 메서드 호출
            // [참고] connector에 선언된 파라미터 구조에 맞게 매핑합니다.
            String result = legacyEimsConnector.executeByTool(
                    params.getRoutingType(),
                    params.getInterfaceId(),
                    params.getData(),
                    params.getSpec()
            );

            // 3. 성공 응답 생성 (result가 JSON 문자열일 경우, 실제 DTO로 변환하여 반환하면 더 좋습니다)
            return createSuccessResponse(request.getId(), result);

        } catch (Exception e) {
            log.error(" [ToolExecution] 커넥터 호출 실패: RoutingType={}, Error={}", params.getRoutingType(), e.getMessage());
            return createErrorResponse(request.getId(), -32000, "Connector error: " + e.getMessage());
        }
    }

    /**
     * AI Agent를 위한 툴 목록 스키마 반환
     */
    public JsonRpcResponse getToolList(String requestId) {
        // 기존에 정의한 Tool 스키마 리스트 반환 로직...
        Map<String, Object> result = new HashMap<>();
        // ... (Tool 명세 내용)
        return createSuccessResponse(requestId, result);
    }

    private JsonRpcResponse createSuccessResponse(String id, Object result) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(id);
        response.setResult(result);
        return response;
    }

    private JsonRpcResponse createErrorResponse(String id, int code, String message) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(id);
        response.setError(new ErrorDetail(code, message));
        return response;
    }
}
