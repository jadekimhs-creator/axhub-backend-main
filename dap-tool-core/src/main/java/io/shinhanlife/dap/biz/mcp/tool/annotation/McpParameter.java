package io.shinhanlife.dap.biz.mcp.tool.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpParameter {
    String description();
    boolean required() default false;
}
