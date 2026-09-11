package io.shinhanlife.dat.lib.metadata;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Tool 정의가 BC-DAB-STD-003 V17 필수 규칙을 만족하는지 검증합니다. */
public final class ToolDefinitionValidator {
    private static final Pattern TOOL_NAME = Pattern.compile("^[a-z][a-z0-9_]{2,63}$");

    private ToolDefinitionValidator() {
    }

    public static void validate(ToolDefinition definition, String source) {
        if (definition == null) {
            fail(source, "definition", "문서가 비어 있습니다");
        }
        if (isBlank(definition.name()) || !TOOL_NAME.matcher(definition.name()).matches()) {
            fail(source, "name", "^[a-z][a-z0-9_]{2,63}$ 형식이어야 합니다");
        }
        requireText(source, "display_name", definition.displayName());
        requireText(source, "version", definition.version());
        requireText(source, "category_key", definition.categoryKey());
        requireText(source, "display_description", definition.displayDescription());
        if (definition.description() == null) {
            fail(source, "description", "필수입니다");
        }
        requireText(source, "description.function", definition.description().function());
        requireText(source, "description.when_to_use", definition.description().whenToUse());
        requireText(source, "description.when_not_to_use", definition.description().whenNotToUse());
        requireText(source, "description.io_limits", definition.description().ioLimits());
        List<String> examples = definition.exampleQueries();
        if (examples == null || examples.size() < 3 || examples.size() > 10
                || examples.stream().anyMatch(ToolDefinitionValidator::isBlank)) {
            fail(source, "example_queries", "비어 있지 않은 자연어 질의가 3~10개 필요합니다");
        }
        if (examples.stream().anyMatch(query -> query.contains(definition.name()))) {
            fail(source, "example_queries", "Tool name을 직접 포함할 수 없습니다");
        }
        if (definition.readOnly() == null || definition.destructive() == null || definition.idempotent() == null) {
            fail(source, "annotations", "read_only, destructive, idempotent는 필수입니다");
        }
        validateSchema(definition.parametersSchema(), source);
        if (definition.outputSchema() != null && !definition.outputSchema().isEmpty()) {
            validateSchema(definition.outputSchema(), source + " output_schema");
        }
    }

    @SuppressWarnings("unchecked")
    private static void validateSchema(Map<String, Object> schema, String source) {
        if (schema == null || !"object".equals(schema.get("type"))) {
            fail(source, "parameters_schema.type", "object여야 합니다");
        }
        if (!Boolean.FALSE.equals(schema.get("additionalProperties"))) {
            fail(source, "parameters_schema.additionalProperties", "false여야 합니다");
        }
        Object propertiesValue = schema.get("properties");
        if (!(propertiesValue instanceof Map<?, ?>)) {
            fail(source, "parameters_schema.properties", "object여야 합니다");
        }
        Map<?, ?> properties = (Map<?, ?>) propertiesValue;
        for (Map.Entry<?, ?> entry : properties.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> property)
                    || isBlank(String.valueOf(property.containsKey("description")
                    ? property.get("description") : ""))) {
                fail(source, "parameters_schema.properties." + entry.getKey() + ".description", "필수입니다");
            }
        }
    }

    private static void requireText(String source, String field, String value) {
        if (isBlank(value)) {
            fail(source, field, "필수입니다");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void fail(String source, String field, String message) {
        throw new IllegalStateException("Invalid Tool definition [" + source + "] " + field + ": " + message);
    }
}
