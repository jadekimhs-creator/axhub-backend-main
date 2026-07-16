package io.shinhanlife.dap.biz.mcp.gateway.resilience;

/**
 * MCP Monitor 화면에 내려주는 Circuit Breaker 상태 정보입니다.
 */
public record CircuitBreakerSnapshot(
        String name,
        String state,
        int timeoutFailures,
        int serverErrorFailures,
        int timeoutOpenThreshold,
        int serverErrorOpenThreshold,
        String lastFailureType,
        String openedReason,
        String openedAt,
        long remainingOpenMillis
) {
}
