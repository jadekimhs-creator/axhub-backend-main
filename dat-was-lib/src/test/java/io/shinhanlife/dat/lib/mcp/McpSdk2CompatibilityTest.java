package io.shinhanlife.dat.lib.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;
import org.junit.jupiter.api.Test;

class McpSdk2CompatibilityTest {

    @Test
    void usesMcpJavaSdkTwo() {
        assertEquals("2.0.0", McpSchema.class.getPackage().getImplementationVersion());
    }

    @Test
    void validatesJsonSchemaWithTheNetworkntVersionRequiredByMcpSdkTwo() {
        DefaultJsonSchemaValidator validator = new DefaultJsonSchemaValidator();

        var response = validator.validateSchema(Map.of(
                "$schema", McpSchema.JSON_SCHEMA_DIALECT_2020_12,
                "type", "object",
                "properties", Map.of("employeeId", Map.of("type", "string"))));

        assertTrue(response.valid(), response.errorMessage());
    }
}
