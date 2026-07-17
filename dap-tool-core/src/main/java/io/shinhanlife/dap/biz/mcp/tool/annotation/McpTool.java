package io.shinhanlife.dap.biz.mcp.tool.annotation;


/**
 * @package io.shinhanlife.dap.biz.mcp.tool.annotation
 * @className McpTool
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
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

    String categoryKey() default "common";
    String routingType() default "HTTP";
}
