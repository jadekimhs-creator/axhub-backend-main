package io.shinhanlife.glow;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlowMciFieldInfo {

    /**
     * 필드 순서
     */
    int order();

    /**
     * 필드 길이 (List인 경우 전체 길이로 활용될 수 있음)
     */
    int length();

    /**
     * 필드 설명
     */
    String description() default "";

    /**
     * List 매핑 시 대상 DTO 클래스
     */
    Class<?> target() default void.class;
}
