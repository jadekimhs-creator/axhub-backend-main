package io.shinhanlife.dat.lib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dat.lib.annotation.McpOutputSchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.annotation.McpTool;

class ToolSchemaResolverTest {

    private final ToolSchemaResolver resolver = new ToolSchemaResolver(new ObjectMapper());

    @Test
    void generatesSchemaFromRequestDtoWhenNoExplicitSchemaIsConfigured() throws Exception {
        Method method = AutomaticSchemaTool.class.getDeclaredMethod("search", AutomaticRequest.class);

        Map<String, Object> schema = resolver.resolve(method.getAnnotation(McpTool.class), null, AutomaticRequest.class);

        assertTrue(properties(schema).containsKey("differentField"));
    }

    @Test
    void generatesOutputSchemaFromMarkedResponseDto() throws Exception {
        Method method = AutomaticOutputSchemaTool.class.getDeclaredMethod("search", AutomaticRequest.class);

        Map<String, Object> schema = resolver.resolveOutput(
                method.getAnnotation(McpTool.class), SimpleResponse.class);

        assertEquals(List.of("resultCode"), schema.get("required"));
        assertEquals(List.of("SUCCESS", "FAILURE"), property(schema, "resultCode").get("enum"));
    }

    @Test
    void doesNotEnableOutputValidationWhenOutputSchemaIsNotDeclared() throws Exception {
        Method method = AutomaticSchemaTool.class.getDeclaredMethod("search", AutomaticRequest.class);

        Map<String, Object> schema = resolver.resolveOutput(
                method.getAnnotation(McpTool.class), AutomaticRequest.class);

        assertTrue(schema.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> properties(Map<String, Object> schema) {
        return (Map<String, Object>) schema.get("properties");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> property(Map<String, Object> schema, String name) {
        return (Map<String, Object>) properties(schema).get(name);
    }

    static class AutomaticSchemaTool {
        @McpTool(name = "oth_test_automatic_search")
        void search(AutomaticRequest request) {
        }
    }

    static class AutomaticOutputSchemaTool {
        @McpTool(name = "oth_test_output_search")
        SimpleResponse search(AutomaticRequest request) {
            return null;
        }
    }

    @McpOutputSchema
    static class SimpleResponse {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"SUCCESS", "FAILURE"})
        private String resultCode;

        private String message;
    }

    static class AutomaticRequest {
        private String differentField;
    }
}