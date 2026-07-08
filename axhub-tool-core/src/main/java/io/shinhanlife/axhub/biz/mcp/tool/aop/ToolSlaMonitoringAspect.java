package io.shinhanlife.axhub.biz.mcp.tool.aop;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.config.McpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ToolSlaMonitoringAspect {

    private final McpProperties mcpProperties;

    // @McpFunction 어노테이션이 붙은 모든 비즈니스 툴 메서드 실행을 가로챕니다.
    @Around("@annotation(io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction)")
    public Object monitorToolSla(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        McpFunction functionAnnotation = method.getAnnotation(McpFunction.class);

        // 네임스페이스 자동 주입 로직을 반영하여 최종 툴 이름을 산출합니다.
        String baseName = functionAnnotation.name();
        String finalName = mcpProperties.getNamespace() != null && !mcpProperties.getNamespace().isEmpty()
                ? mcpProperties.getNamespace() + "_" + baseName
                : baseName;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            // 실제 비즈니스 로직(툴) 실행
            Object result = joinPoint.proceed();
            
            stopWatch.stop();
            long timeMillis = stopWatch.getTotalTimeMillis();
            
            // SLA 기준을 초과하면 (예: 2초 이상) 경고 로깅 처리 가능
            if (timeMillis > 2000) {
                log.warn("🚨 [SLA 경고] Tool: {} | 소요시간: {}ms | 상태: SLOW_RESPONSE", finalName, timeMillis);
            } else {
                log.info("📊 [SLA 추적] Tool: {} | 소요시간: {}ms | 상태: SUCCESS", finalName, timeMillis);
            }
            
            return result;
            
        } catch (Throwable e) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            long timeMillis = stopWatch.getTotalTimeMillis();
            
            // 에러 발생 시 명확하게 실패 로그 기록
            log.error("💥 [SLA 장애] Tool: {} | 소요시간: {}ms | 상태: FAILED | 사유: {}", finalName, timeMillis, e.getMessage());
            
            // 원래 흐름대로 예외를 던져서 게이트웨이나 상위 로직이 에러를 처리하게 함
            throw e;
        }
    }
}
