package io.shinhanlife.dat.mcg.resilience;


/**
 * @package io.shinhanlife.dat.mcg.resilience
 * @className FailureType
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 * 
 * </pre>
 */
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
