package io.shinhanlife.dat.mcc.presentation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.shinhanlife.dat.lib.validation.ToolArgumentSchemaValidator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolArgumentSchemaValidatorTest {

    private final ToolArgumentSchemaValidator validator =
            new ToolArgumentSchemaValidator(new ObjectMapper(), new DefaultJsonSchemaValidator());

    @Test
    void validatesJsonSchema202012WithTheMcpSdkValidator() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("name", Map.of("type", "string")),
                "required", List.of("name"));

        assertTrue(validator.validate(schema, Map.of("name", "Hong")).valid());
        assertFalse(validator.validate(schema, Map.of()).valid());
    }
}
