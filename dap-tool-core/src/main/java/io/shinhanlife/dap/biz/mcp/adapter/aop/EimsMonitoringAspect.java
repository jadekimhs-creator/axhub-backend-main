package io.shinhanlife.dap.biz.mcp.adapter.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @package io.shinhanlife.dap.biz.mcp.adapter.aop
 * @className EimsMonitoringAspect
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
public class EimsMonitoringAspect {

    //  포인트컷: io.shinhanlife.dap.biz.mcp.adapter.sender 패키지 내의 EimsSender를 구현한 모든 클래스의 메서드를 타겟으로 지정합니다.
    @Around("execution(* io.shinhanlife.dap.biz.mcp.adapter.sender.*EimsSender.*(..))")
    public Object monitorEimsCommunication(ProceedingJoinPoint joinPoint) throws Throwable {

        // 1. 호출되는 클래스와 메서드 이름 추출
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs(); // 파라미터

        long startTime = System.currentTimeMillis();

        // 2. [요청 로깅] EIMS망으로 요청이 나가기 직전
        log.info(" [EIMS 요청] {} - {}() | Params: {}", className, methodName, Arrays.toString(args));

        try {
            //  실제 레거시 통신 로직 실행 (이 코드가 없으면 통신이 진행되지 않습니다)
            Object result = joinPoint.proceed();

            // 3. [응답 로깅] 정상적으로 통신이 완료된 후
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("◀ [EIMS 응답] {} - {}() | 소요시간: {}ms | Result: {}", className, methodName, executionTime, result);

            return result;

        } catch (Exception e) {
            // 4. [에러 로깅] 레거시 통신 중 장애(타임아웃 등) 발생 시
            long executionTime = System.currentTimeMillis() - startTime;
            log.error(" [EIMS 에러] {} - {}() | 소요시간: {}ms | Error: {}", className, methodName, executionTime, e.getMessage());

            // 에러를 삼키지 않고 다시 던져서 기존 예외 처리 로직(서킷 브레이커 등)이 작동하게 합니다.
            throw e;
        }
    }
}