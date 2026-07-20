package io.shinhanlife.dap.mcg.resilience;


/**
 * @package io.shinhanlife.dap.mcg.resilience
 * @className CircuitBreakerSnapshot
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
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
