package io.shinhanlife.axhub.biz.mcp.gateway.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * @package io.shinhanlife.axhub.biz.mcp.gateway.aop
 * @className GatewayLoggingAspect
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
@Slf4j
@Aspect
@Component
public class GatewayLoggingAspect {

    // gateway의 controller 패키지 하위의 모든 클래스/메서드 실행 시 작동
    @Around("execution(* io.shinhanlife.axhub.biz.mcp.gateway.controller..*(..))")
    public Object logGatewayExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String targetMethod = joinPoint.getSignature().toShortString();
        StopWatch stopWatch = new StopWatch();

        log.info(" [Gateway 송신 시작] Target: {}", targetMethod);
        stopWatch.start();

        try {
            // 실제 대상 메서드(외부 통신) 실행
            Object result = joinPoint.proceed();
            return result;
        } finally {
            stopWatch.stop();
            long timeMillis = stopWatch.getTotalTimeMillis();

            if (timeMillis > 3000) { // 3초 이상 걸리면 WARN 로그로 슬로우 통신 경고
                log.warn("⏱ [Gateway 슬로우 응답] Target: {} | 소요시간: {}ms", targetMethod, timeMillis);
            } else {
                log.info("⏹ [Gateway 송신 완료] Target: {} | 소요시간: {}ms", targetMethod, timeMillis);
            }
        }
    }
}