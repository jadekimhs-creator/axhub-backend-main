package io.shinhanlife.axhub.biz.mcp.adapter.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Aspect
@Component
public class GatewayLoggingAspect {

    // connector 패키지 하위의 모든 클래스/메서드 실행 시 작동
    @Around("execution(* com.shinhan.mcp.adapter.controller..*(..)) " +
            "|| execution(* com.shinhan.mcp.adapter.connector..*(..))")
    public Object logConnectorExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
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