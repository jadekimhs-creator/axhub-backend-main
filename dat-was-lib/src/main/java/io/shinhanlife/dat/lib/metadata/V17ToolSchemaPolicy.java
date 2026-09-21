package io.shinhanlife.dat.lib.metadata;

import io.shinhanlife.dat.lib.util.ToolScaffolder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared, deterministic rules for Tool Function V17 metadata.
 */
public final class V17ToolSchemaPolicy {

    private static final Set<String> READ_ACTIONS = Set.of("search", "detail");
    private static final String PAGING_DESCRIPTION = "페이징 처리 객체 (생략 시 시스템 기본값 적용)";

    private V17ToolSchemaPolicy() {
    }

    public static String normalizeJavaBaseName(String rawBaseName, String categoryKey) {
        String normalizedCategory = normalizeCategory(categoryKey);
        List<String> parts = splitName(rawBaseName);
        if (parts.size() >= 3 && parts.getFirst().equalsIgnoreCase(normalizedCategory)) {
            parts = parts.subList(1, parts.size());
        }
        if (parts.size() < 2 || !READ_ACTIONS.contains(parts.getFirst().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("V17 Tool 기능어는 search 또는 detail만 사용할 수 있습니다.");
        }

        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            String lowerCasePart = part.toLowerCase(Locale.ROOT);
            result.append(Character.toUpperCase(lowerCasePart.charAt(0))).append(lowerCasePart.substring(1));
        }
        return result.toString();
    }

    public static List<String> validateExamples(List<String> examples) {
        List<String> normalized = normalizeTextList(examples);
        if (normalized.size() < 3 || normalized.size() > 10) {
            throw new IllegalArgumentException("V17 exampleQueries는 3~10개여야 합니다.");
        }
        return normalized;
    }

    public static List<String> validateTags(List<String> tags, String categoryKey) {
        String normalizedCategory = normalizeCategory(categoryKey);
        List<String> normalized = normalizeTextList(tags);
        if (normalized.size() < 5 || normalized.size() > 8) {
            throw new IllegalArgumentException("V17 tags는 5~8개여야 합니다.");
        }
        if (!normalizedCategory.equalsIgnoreCase(normalized.getFirst())) {
            throw new IllegalArgumentException("V17 tags의 첫 번째 값은 categoryKey여야 합니다.");
        }
        normalized.set(0, normalizedCategory);
        return List.copyOf(normalized);
    }

    public static List<ToolScaffolder.FieldDefinition> normalizePagingFields(
            List<ToolScaffolder.FieldDefinition> fields) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        return fields.stream().map(field -> {
            if (field == null || field.name() == null) {
                return field;
            }
            if (!"pageInfo".equalsIgnoreCase(field.name()) && !"scrPageInfo".equalsIgnoreCase(field.name())) {
                return field;
            }
            String canonicalName = "pageInfo".equalsIgnoreCase(field.name()) ? "pageInfo" : "scrPageInfo";
            return new ToolScaffolder.FieldDefinition(canonicalName, field.type(), PAGING_DESCRIPTION,
                    field.examples(), field.pattern(), false, field.enumValues(), field.itemType(), field.itemFields());
        }).toList();
    }

    private static String normalizeCategory(String categoryKey) {
        String normalized = categoryKey == null ? "" : categoryKey.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-z0-9]{3}$")) {
            throw new IllegalArgumentException("V17 categoryKey는 영문 소문자 또는 숫자 3자여야 합니다.");
        }
        return normalized;
    }

    private static List<String> splitName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("V17 Base Name은 필수입니다.");
        }
        String normalized = value.trim().replace('-', '_');
        List<String> parts = new ArrayList<>();
        for (String token : normalized.split("_+")) {
            if (token.isBlank()) {
                continue;
            }
            for (String camelPart : token.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")) {
                if (!camelPart.isBlank()) {
                    parts.add(camelPart);
                }
            }
        }
        return parts;
    }

    private static List<String> normalizeTextList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList()));
    }
}
