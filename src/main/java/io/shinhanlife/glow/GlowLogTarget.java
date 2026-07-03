package io.shinhanlife.glow;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlowLogTarget {

    Target[] value() default {};

    enum Target {
        FILE, CONSOLE
    }
}
