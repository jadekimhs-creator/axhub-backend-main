package io.shinhanlife.glow;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlowAppServiceId {

    String value();

    String description() default "";
}