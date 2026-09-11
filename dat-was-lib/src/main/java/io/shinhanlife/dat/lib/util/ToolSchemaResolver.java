package io.shinhanlife.dat.lib.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dat.lib.annotation.McpOutputSchema;
import io.shinhanlife.dat.lib.annotation.GrowToolHint;
import java.io.InputStream;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

/** Resolves MCP Tool schemas from resources or DTO metadata. */
public class ToolSchemaResolver {

    private final ObjectMapper objectMapper;

    public ToolSchemaResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> resolve(org.springaicommunity.mcp.annotation.McpTool function,
                                       GrowToolHint hint, Class<?> requestType) {
        if (hint != null && !hint.inputSchemaResource().isBlank()) {
            return loadResource(hint.inputSchemaResource());
        }
        return JsonSchemaGenerator.generateSchema(requestType);
    }

    /**
     * Resolves a response schema only when it is explicitly declared.
     * A JSON resource has precedence over a DTO marker annotation.
     */
    public Map<String, Object> resolveOutput(org.springaicommunity.mcp.annotation.McpTool function,
                                             Class<?> responseType, GrowToolHint hint) {
        if (hint != null && !hint.outputSchemaResource().isBlank()) {
            return loadResource(hint.outputSchemaResource());
        }
        return resolveOutput(function, responseType);
    }

    /**
     * Generates a response schema only for DTOs marked with {@link McpOutputSchema}.
     */
    public Map<String, Object> resolveOutput(org.springaicommunity.mcp.annotation.McpTool function,
                                             Class<?> responseType) {
        if (responseType == null
                || responseType == Object.class
                || Map.class.isAssignableFrom(responseType)
                || responseType == Void.class
                || responseType == void.class
                || !responseType.isAnnotationPresent(McpOutputSchema.class)) {
            return Map.of();
        }
        return JsonSchemaGenerator.generateSchema(responseType);
    }

    /**
     * Retained for callers that use only explicit output schemas.
     */
    public Map<String, Object> resolveOutput(org.springaicommunity.mcp.annotation.McpTool function) {
        return Map.of();
    }

    private Map<String, Object> loadResource(String location) {
        String path = location.startsWith("classpath:")
                ? location.substring("classpath:".length())
                : location;
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException("MCP schema resource not found: " + location);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() { });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load MCP schema resource: " + location, e);
        }
    }
}