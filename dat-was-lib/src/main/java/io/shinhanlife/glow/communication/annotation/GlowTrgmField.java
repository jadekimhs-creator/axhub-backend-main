package io.shinhanlife.glow.communication.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares fixed-width Glow transaction-message metadata for a DTO field. */
@Target({ElementType.LOCAL_VARIABLE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlowTrgmField {

    int order();

    int length() default 0;

    int decimal() default 0;

    String description() default "";

    String target() default "";

    String type() default "";
}