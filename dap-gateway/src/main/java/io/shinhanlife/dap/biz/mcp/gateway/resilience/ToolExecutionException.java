package io.shinhanlife.dap.biz.mcp.gateway.resilience;

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
