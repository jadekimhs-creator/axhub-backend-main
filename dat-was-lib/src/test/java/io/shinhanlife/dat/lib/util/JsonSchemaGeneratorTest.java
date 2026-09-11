package io.shinhanlife.dat.lib.util;


import org.springaicommunity.mcp.annotation.McpToolParam;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonSchemaGeneratorTest {

    @Test
    void includesMcpParameterConstraintsInGeneratedSchema() {
        Map<String, Object> schema = JsonSchemaGenerator.generateSchema(ValidatedRequest.class);
        Map<String, Map<String, Object>> properties = properties(schema);

        assertTrue(((List<String>) schema.get("required")).contains("phoneNumber"));
        assertTrue(((List<String>) schema.get("required")).contains("approvalStatus"));
        assertEquals("^01[0-9]{8,9}$", properties.get("phoneNumber").get("pattern"));
        assertEquals(1L, properties.get("amount").get("minimum"));
        assertEquals(List.of("APPROVE", "REJECT"), properties.get("approvalStatus").get("enum"));
    }

    @Test
    void includesExtendedMcpValidationConstraintsInGeneratedSchema() {
        Map<String, Object> schema = JsonSchemaGenerator.generateSchema(ValidatedRequest.class);

        assertEquals(50L, property(schema, "pageSize").get("maximum"));
        assertEquals(20L, property(schema, "pageSize").get("default"));
        assertEquals(1, property(schema, "reference").get("minLength"));
        assertEquals(30, property(schema, "reference").get("maxLength"));
    }

    @Test
    void includesNestedDtoConstraintsInGeneratedSchema() {
        Map<String, Object> schema = JsonSchemaGenerator.generateSchema(NestedRequest.class);
        Map<String, Object> childSchema = property(schema, "child");

        assertEquals("object", childSchema.get("type"));
        assertTrue(required(childSchema).contains("businessDate"));
        assertEquals("^\\\\d{8}$", property(childSchema, "businessDate").get("pattern"));
    }

    @Test
    void includesNestedDtoSchemaForListItems() {
        Map<String, Object> schema = JsonSchemaGenerator.generateSchema(ListRequest.class);
        Map<String, Object> itemSchema = map(property(schema, "items").get("items"));

        assertEquals("object", itemSchema.get("type"));
        assertTrue(required(itemSchema).contains("businessDate"));
    }

    @Test
    void validatorRejectsInvalidNestedValue() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonSchemaValidator validator = new DefaultJsonSchemaValidator();
        Map<String, Object> schema = JsonSchemaGenerator.generateSchema(NestedRequest.class);
        Object arguments = objectMapper.convertValue(
                Map.of("child", Map.of("businessDate", "2026-07-28")), Object.class);
        JsonSchemaValidator.ValidationResponse result = validator.validate(schema, arguments);

        assertFalse(result.valid());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> properties(Map<String, Object> schema) {
        return (Map<String, Map<String, Object>>) schema.get("properties");
    }

    private Map<String, Object> property(Map<String, Object> schema, String name) {
        return properties(schema).get(name);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> required(Map<String, Object> schema) {
        return (List<String>) schema.get("required");
    }

    private static class ValidatedRequest {
        @McpToolParam(description = "recipient phone number", required = true)
        @io.swagger.v3.oas.annotations.media.Schema(pattern = "^01[0-9]{8,9}$")
        private String phoneNumber;

        @McpToolParam(description = "issue amount", required = true)
        @io.swagger.v3.oas.annotations.media.Schema(minimum = "1")
        private Long amount;

        @McpToolParam(description = "approval result")
        @io.swagger.v3.oas.annotations.media.Schema(
                requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED,
                allowableValues = {"APPROVE", "REJECT"})
        private String approvalStatus;

        @McpToolParam(description = "page size")
        @io.swagger.v3.oas.annotations.media.Schema(maximum = "50", defaultValue = "20")
        private Integer pageSize;

        @McpToolParam(description = "reference")
        @io.swagger.v3.oas.annotations.media.Schema(minLength = 1, maxLength = 30)
        private String reference;
    }

    private static class NestedRequest {
        private NestedChild child;
    }

    private static class ListRequest {
        private List<NestedChild> items;
    }

    private static class NestedChild {
        @io.swagger.v3.oas.annotations.media.Schema(
                requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED,
                pattern = "^\\\\d{8}$")
        private String businessDate;
    }
}
