package io.shinhanlife.glow;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
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
    int length();

    /**
     * 필드 설명
     */
    String description() default "";

    /**
     * List 매핑 시 대상 클래스 (제네릭 타입 소거 우회용)
     */
    Class<?> target() default void.class;
}
