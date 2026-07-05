package io.shinhanlife.axhub.biz.mcp.tool.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface McpTool {
    @AliasFor(annotation = Component.class)
    String value() default "";

    String group() default "COMMON";
    String routingType() default "HTTP";
}
