package io.shinhanlife.dat.lib.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry 메타데이터를 MCP SDK Tool 명세로 일관되게 변환합니다. */
public final class ToolMetadataMcpMapper {
    private ToolMetadataMcpMapper() {
    }

    public static McpSchema.Tool toTool(ToolMetadata metadata) {
        McpSchema.Tool.Builder builder = McpSchema.Tool.builder()
                .name(metadata.getName())
                .title(defaultText(metadata.getDisplayName(), metadata.getName()))
                .description(defaultText(metadata.getDescription(), metadata.getName() + " Tool"))
                .inputSchema(toJsonSchema(metadata.getParametersSchema()))
                .annotations(new McpSchema.ToolAnnotations(
                        metadata.getDisplayName(), metadata.getReadOnlyHint(), metadata.getDestructiveHint(),
                        metadata.getIdempotentHint(), metadata.getOpenWorldHint(), null))
                .meta(meta(metadata));
        if (metadata.getOutputSchema() != null && !metadata.getOutputSchema().isEmpty()) {
            builder.outputSchema(metadata.getOutputSchema());
        }
        return builder.build();
    }

    public static Map<String, Object> meta(ToolMetadata metadata) {
        Map<String, Object> meta = new LinkedHashMap<>();
        put(meta, "version", metadata.getSemver());
        put(meta, "category_key", metadata.getCategoryKey());
        put(meta, "display_description", metadata.getDisplayDescription());
        put(meta, "when_to_use", metadata.getWhenToUse());
        put(meta, "when_not_to_use", metadata.getWhenNotToUse());
        put(meta, "io_limits", metadata.getIoLimits());
        put(meta, "example_queries", metadata.getExampleQueries());
        put(meta, "tags", metadata.getTags());
        put(meta, "legacy_interface_id", metadata.getMciServiceId());
        put(meta, "required_env_keys", metadata.getRequiredEnvKeys());
        put(meta, "owner_org", metadata.getOwnerOrg());
        return Map.copyOf(meta);
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (value instanceof String text && !text.isBlank()) {
            target.put(key, text);
        } else if (value instanceof List<?> list && !list.isEmpty()) {
            target.put(key, List.copyOf(list));
        }
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @SuppressWarnings("unchecked")
    private static McpSchema.JsonSchema toJsonSchema(Map<String, Object> source) {
        Map<String, Object> schema = source == null ? emptySchema() : source;
        return new McpSchema.JsonSchema(
                String.valueOf(schema.getOrDefault("type", "object")),
                schema.get("properties") instanceof Map<?, ?> properties
                        ? (Map<String, Object>) properties : Map.of(),
                schema.get("required") instanceof List<?> required ? (List<String>) required : List.of(),
                schema.get("additionalProperties") instanceof Boolean additionalProperties
                        ? additionalProperties : Boolean.TRUE,
                schema.get("$defs") instanceof Map<?, ?> defs ? (Map<String, Object>) defs : Map.of(),
                schema.get("definitions") instanceof Map<?, ?> definitions
                        ? (Map<String, Object>) definitions : Map.of());
    }

    private static Map<String, Object> emptySchema() {
        return Map.of("type", "object", "properties", Map.of(), "additionalProperties", false);
    }
}
