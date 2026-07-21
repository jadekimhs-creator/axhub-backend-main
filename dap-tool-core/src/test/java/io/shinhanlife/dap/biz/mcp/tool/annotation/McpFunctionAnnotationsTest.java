package io.shinhanlife.dap.biz.mcp.tool.annotation;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class McpFunctionAnnotationsTest {

    @Test
    void usesConservativeDefaultsForMcpBehaviorHints() throws NoSuchMethodException {
        Method method = SampleTool.class.getDeclaredMethod("execute");
        McpFunction annotation = method.getAnnotation(McpFunction.class);

        assertFalse(annotation.readOnlyHint());
        assertFalse(annotation.destructiveHint());
        assertFalse(annotation.idempotentHint());
        assertFalse(annotation.openWorldHint());
    }

    static class SampleTool {
        @McpFunction(displayName = "sample", name = "sample", description = "sample")
        void execute() {
        }
    }
}
