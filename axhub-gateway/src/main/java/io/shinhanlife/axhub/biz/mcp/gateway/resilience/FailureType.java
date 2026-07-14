package io.shinhanlife.axhub.biz.mcp.gateway.resilience;

/**
 * Tool/EIMS/Tool 서버 호출 실패를 업무적으로 구분하기 위한 장애 유형입니다.
 *
 * Retry, Circuit Breaker, 감사 로그에서 같은 기준으로 오류를 판단합니다.
 */
public enum FailureType {
    TIMEOUT,
    CLIENT_ERROR,
    SERVER_ERROR,
    NETWORK_ERROR,
    AUTHORIZATION_ERROR,
    BUSINESS_ERROR,
    HALLUCINATION_GUARDRAIL,
    TOOL_DELETED,
    CIRCUIT_OPEN,
    TOO_LARGE_RESULT,
    INTERNAL_ERROR
}
