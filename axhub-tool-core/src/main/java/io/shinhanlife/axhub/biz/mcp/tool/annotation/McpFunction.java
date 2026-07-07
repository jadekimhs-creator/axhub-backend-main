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
}
