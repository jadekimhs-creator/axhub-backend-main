package io.shinhanlife.dap.biz.mcp.gateway.resilience;

/**
 * Circuit Breaker의 현재 상태입니다.
 *
 * CLOSED는 정상 호출 가능, OPEN은 호출 차단, HALF_OPEN은 복구 확인 상태를 의미합니다.
 */
public enum CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
