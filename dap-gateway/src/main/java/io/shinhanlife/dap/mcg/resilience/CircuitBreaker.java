package io.shinhanlife.dap.mcg.resilience;


/**
 * @package io.shinhanlife.dap.mcg.resilience
 * @className CircuitBreaker
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
import java.time.Clock;
import java.time.Instant;

/**
 * Tool별 장애 확산을 막기 위한 Circuit Breaker입니다.
 *
 * 현재 OPEN 조건은 사용자가 요청한 기준에 맞춰
 * TIMEOUT 3회 이상 또는 SERVER_ERROR(5xx) 3회 이상입니다.
 */
public class CircuitBreaker {
    private static final int TIMEOUT_OPEN_THRESHOLD = 3;
    private static final int SERVER_ERROR_OPEN_THRESHOLD = 3;

    private final String name;
    private final long openDurationMillis;
    private final Clock clock;
    private CircuitBreakerState state = CircuitBreakerState.CLOSED;
    private int timeoutFailures;
    private int serverErrorFailures;
    private FailureType lastFailureType;
    private String openedReason = "";
    private Instant openedAt;

    public CircuitBreaker(String name, int failureThreshold, long openDurationMillis, Clock clock) {
        this.name = name;
        this.openDurationMillis = Math.max(1, openDurationMillis);
        this.clock = clock;
    }

    /**
     * 실제 Tool 호출 전에 현재 회로가 호출 가능한 상태인지 확인합니다.
     */
    public synchronized void beforeCall() {
        if (state != CircuitBreakerState.OPEN) {
            return;
        }
        long openedMillis = openedAt == null ? 0 : clock.millis() - openedAt.toEpochMilli();
        if (openedMillis >= openDurationMillis) {
            state = CircuitBreakerState.HALF_OPEN;
            return;
        }
        throw new ToolExecutionException(FailureType.CIRCUIT_OPEN, "Circuit breaker is open: " + name);
    }

    /**
     * 호출 성공 시 실패 카운터를 초기화하고 CLOSED 상태로 복구합니다.
     */
    public synchronized void recordSuccess() {
        timeoutFailures = 0;
        serverErrorFailures = 0;
        lastFailureType = null;
        openedReason = "";
        openedAt = null;
        state = CircuitBreakerState.CLOSED;
    }

    /**
     * 장애 유형별 실패 횟수를 집계하고 조건에 맞으면 OPEN 상태로 전환합니다.
     */
    public synchronized void recordFailure(FailureType failureType) {
        if (failureType == FailureType.TIMEOUT) {
            timeoutFailures++;
            lastFailureType = failureType;
        } else if (failureType == FailureType.SERVER_ERROR) {
            serverErrorFailures++;
            lastFailureType = failureType;
        } else if (state == CircuitBreakerState.HALF_OPEN && isCircuitBreakerFailure(failureType)) {
            lastFailureType = failureType;
            open("HALF_OPEN_TEST_FAILED");
            return;
        } else {
            return;
        }

        if (state == CircuitBreakerState.HALF_OPEN) {
            open(failureType.name() + "_IN_HALF_OPEN");
            return;
        }
        if (timeoutFailures >= TIMEOUT_OPEN_THRESHOLD) {
            open("TIMEOUT_3_OR_MORE");
        }
        if (serverErrorFailures >= SERVER_ERROR_OPEN_THRESHOLD) {
            open("SERVER_ERROR_5XX_3_OR_MORE");
        }
    }

    public synchronized CircuitBreakerState state() {
        return state;
    }

    /**
     * MCP Monitor 화면에 표시할 현재 상태 스냅샷입니다.
     */
    public synchronized CircuitBreakerSnapshot snapshot() {
        long remainingOpenMillis = 0;
        if (state == CircuitBreakerState.OPEN && openedAt != null) {
            long elapsed = Math.max(0, clock.millis() - openedAt.toEpochMilli());
            remainingOpenMillis = Math.max(0, openDurationMillis - elapsed);
        }
        return new CircuitBreakerSnapshot(
                name,
                state.name(),
                timeoutFailures,
                serverErrorFailures,
                TIMEOUT_OPEN_THRESHOLD,
                SERVER_ERROR_OPEN_THRESHOLD,
                lastFailureType == null ? "" : lastFailureType.name(),
                openedReason,
                openedAt == null ? "" : openedAt.toString(),
                remainingOpenMillis);
    }

    /**
     * 테스트 후 화면에서 회로를 수동 초기화할 때 사용합니다.
     */
    public synchronized void reset() {
        recordSuccess();
    }

    private void open(String reason) {
        state = CircuitBreakerState.OPEN;
        openedAt = Instant.now(clock);
        openedReason = reason;
    }

    private boolean isCircuitBreakerFailure(FailureType failureType) {
        return failureType == FailureType.TIMEOUT
                || failureType == FailureType.NETWORK_ERROR
                || failureType == FailureType.SERVER_ERROR
                || failureType == FailureType.INTERNAL_ERROR;
    }
}
