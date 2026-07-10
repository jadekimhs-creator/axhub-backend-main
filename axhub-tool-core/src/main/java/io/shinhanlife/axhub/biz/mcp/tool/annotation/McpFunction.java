package io.shinhanlife.axhub.biz.mcp.tool.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpFunction {
    String name();
    String description();
    String prompt() default "";
    String mappingId() default "";
    
    // 추가: 해당 함수가 요구하는 비즈니스 파라미터(JSON 형태의 properties)를 정의
    String parameterSchema() default "{}";
    
    // 추가: Redis 자동 등록 및 Heartbeat 대상 여부 제어
    boolean register() default true;
    
    // 추가: 툴 목록 노출 여부 제어 (false 시 라우팅은 되나 목록에서 숨김)
    boolean visible() default true;

    // 추가: HITL 승인 체계 지원 (실행 전 사용자 승인 필요 여부)
    boolean requiresApproval() default false;
}
