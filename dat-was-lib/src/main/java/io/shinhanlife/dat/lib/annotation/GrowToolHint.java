package io.shinhanlife.dat.lib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Spring AI @Tool 어노테이션을 보완하여 MCP 시스템 메타데이터를 추가 제공하는 힌트 어노테이션
 * @package io.shinhanlife.dat.lib.annotation
 * @className GrowToolHint
 * @description 비즈니스 로직(Tool)과 시스템 제어 메타데이터 분리
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
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrowToolHint {
    boolean register() default true;
    boolean requiresApproval() default false;
    String categoryKey() default "com";
    String mappingId() default "";
    String inputSchemaResource() default "";
    String outputSchemaResource() default "";
    long timeoutMillis() default 5000L;
    int retryMaxAttempts() default 3;

    // Meta 정보 추가 (보고용 샘플)
    String displayDescription() default "";
    String functionDescription() default "";
    String whenToUse() default "";
    String whenNotToUse() default "";
    String ioLimits() default "";
    String[] exampleQueries() default {};
    boolean destructive() default false;
    boolean idempotent() default false;
    String[] tags() default {};
    String[] requiredEnvKeys() default {};
    String ownerOrg() default "";
}
