package io.shinhanlife.glow;

import java.lang.annotation.*;

@Target({ElementType.LOCAL_VARIABLE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlowTrgmField {

    /**
     * 필드 순서
     */
    int order();

    /**
     * 필드 길이
     */
    int length() default 0;

    /**
     * 소수점 자리수
     */
    int decimal() default 0;

    /**
     * 필드 설명
     */
    String description() default "";

    /**
     * 타겟
     */
    String target() default "";

    /**
     * 필드 타입 (예: gm)
     */
    String type() default "";

}

