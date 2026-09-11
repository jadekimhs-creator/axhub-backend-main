package io.shinhanlife.dat.lib.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.shinhanlife.dat.lib.util.ToolSchemaResolver;
import io.shinhanlife.dat.lib.validation.ToolArgumentSchemaValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Common MCP Tool Schema Bean configuration.
 */
@Configuration
public class ToolSchemaConfiguration {

    @Bean
    public ToolSchemaResolver toolSchemaResolver(ObjectMapper objectMapper) {
        return new ToolSchemaResolver(objectMapper);
    }

    @Bean
    public JsonSchemaValidator mcpJsonSchemaValidator() {
        return new DefaultJsonSchemaValidator();
    }

    @Bean
    public ToolArgumentSchemaValidator toolArgumentSchemaValidator(ObjectMapper objectMapper,
                                                                   JsonSchemaValidator jsonSchemaValidator) {
        return new ToolArgumentSchemaValidator(objectMapper, jsonSchemaValidator);
    }
}
