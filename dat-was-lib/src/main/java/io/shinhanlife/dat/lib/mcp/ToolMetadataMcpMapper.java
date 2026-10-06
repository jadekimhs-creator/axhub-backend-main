package io.shinhanlife.dat.lib.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import java.util.ArrayList;
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
        meta.put("timeoutMillis", positiveOrDefault(metadata.getTimeoutMillis(), 5000L));
        meta.put("retryMaxAttempts", positiveOrDefault(metadata.getRetryMaxAttempts(), 3));
        return Map.copyOf(meta);
    }

    private static long positiveOrDefault(Long value, long defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
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

    /**
     * DTO명 또는 Tool명으로 중첩 래핑된 스키마를 최상위 레벨로 평탄화(Flatten)합니다.
     * Agent Builder 및 MCP 규격에 맞게 properties 및 required를 최상위 레벨로 승격합니다.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalizeSchema(Map<String, Object> source) {
        if (source == null) {
            return emptySchema();
        }
        Map<String, Object> schema = new LinkedHashMap<>(source);
        if (schema.get("properties") instanceof Map<?, ?> props && props.size() == 1) {
            Map.Entry<?, ?> entry = props.entrySet().iterator().next();
            if (entry.getValue() instanceof Map<?, ?> nested && nested.get("properties") instanceof Map<?, ?> nestedProps) {
                schema.put("properties", new LinkedHashMap<>((Map<String, Object>) nestedProps));

                List<String> combinedRequired = new ArrayList<>();
                if (schema.get("required") instanceof List<?> currentRequired) {
                    for (Object req : currentRequired) {
                        if (req instanceof String s && !s.equals(entry.getKey())) {
                            combinedRequired.add(s);
                        }
                    }
                }
                if (nested.get("required") instanceof List<?> nestedRequired) {
                    for (Object req : nestedRequired) {
                        if (req instanceof String s && !combinedRequired.contains(s)) {
                            combinedRequired.add(s);
                        }
                    }
                }
                if (!combinedRequired.isEmpty()) {
                    schema.put("required", combinedRequired);
                }
            }
        }
        return schema;
    }

    @SuppressWarnings("unchecked")
    private static McpSchema.JsonSchema toJsonSchema(Map<String, Object> source) {
        Map<String, Object> schema = normalizeSchema(source);
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
