package io.shinhanlife.dat.mcg.resilience;


/**
 * @package io.shinhanlife.dat.mcg.resilience
 * @className ToolExecutionException
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
 * Tool 실행 중 발생한 오류를 FailureType과 함께 전달하는 공통 예외입니다.
 *
 * 상위 GatewayToolExecutor가 이 예외를 기준으로 감사 로그, Redis Trace, 재시도 여부를 판단합니다.
 */
public class ToolExecutionException extends RuntimeException {
    private final FailureType failureType;

    public ToolExecutionException(String message) {
        this(FailureType.BUSINESS_ERROR, message);
    }

    public ToolExecutionException(FailureType failureType, String message) {
        super(message);
        this.failureType = failureType;
    }

    public ToolExecutionException(FailureType failureType, String message, Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
    }

    public FailureType failureType() {
        return failureType;
    }
}
