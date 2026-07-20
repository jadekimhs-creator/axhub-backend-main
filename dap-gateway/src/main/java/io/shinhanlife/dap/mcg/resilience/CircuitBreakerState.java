package io.shinhanlife.dap.mcg.resilience;


/**
 * @package io.shinhanlife.dap.mcg.resilience
 * @className CircuitBreakerState
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
 * Circuit Breaker의 현재 상태입니다.
 *
 * CLOSED는 정상 호출 가능, OPEN은 호출 차단, HALF_OPEN은 복구 확인 상태를 의미합니다.
 */
public enum CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
