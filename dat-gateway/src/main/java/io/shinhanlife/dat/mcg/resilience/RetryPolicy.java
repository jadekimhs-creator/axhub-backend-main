package io.shinhanlife.dat.mcg.resilience;


/**
 * @package io.shinhanlife.dat.mcg.resilience
 * @className RetryPolicy
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
 * Tool 호출 재시도 정책입니다.
 *
 * 최대 재시도 횟수, 최초 대기 시간, Backoff 배율, 최대 대기 시간을 담습니다.
 */
public record RetryPolicy(int maxAttempts, long initialBackoffMillis, double backoffMultiplier, long maxBackoffMillis) {
    /**
     * WRITE 요청처럼 재시도하면 위험한 경우 사용하는 비활성 정책입니다.
     */
    public static RetryPolicy disabled() {
        return new RetryPolicy(1, 0, 1.0, 0);
    }

    /**
     * 실패한 시도 횟수에 따라 다음 재시도 전 대기 시간을 계산합니다.
     */
    public long backoffMillis(int failedAttempt) {
        if (failedAttempt < 1 || initialBackoffMillis < 1) {
            return 0;
        }
        double backoff = initialBackoffMillis * Math.pow(backoffMultiplier, failedAttempt - 1);
        if (maxBackoffMillis > 0) {
            backoff = Math.min(backoff, maxBackoffMillis);
        }
        return Math.max(0, Math.round(backoff));
    }
}
