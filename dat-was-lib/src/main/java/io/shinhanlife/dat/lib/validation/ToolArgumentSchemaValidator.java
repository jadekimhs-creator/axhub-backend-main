package io.shinhanlife.dat.lib.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import java.util.Map;

/** Validates tool arguments with the MCP SDK JsonSchemaValidator (JSON Schema 2020-12). */
public class ToolArgumentSchemaValidator {

    private final ObjectMapper objectMapper;
    private final JsonSchemaValidator jsonSchemaValidator;

    public ToolArgumentSchemaValidator(ObjectMapper objectMapper, JsonSchemaValidator jsonSchemaValidator) {
        this.objectMapper = objectMapper;
        this.jsonSchemaValidator = jsonSchemaValidator;
    }

    public ValidationResponse validate(Map<String, Object> schemaDefinition, Map<String, Object> arguments) {
        return validateValue(schemaDefinition, arguments);
    }

    public ValidationResponse validateValue(Map<String, Object> schemaDefinition, Object value) {
        Object converted = objectMapper.convertValue(value, Object.class);
        return jsonSchemaValidator.validate(schemaDefinition, converted);
    }
}
