package io.shinhanlife.dat.lib.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Glow MCI 전문({@code *_I.java}/{@code *_O.java})을 분석하여 LLM 노출용 Request/Response DTO와
 * MapStruct Converter 소스를 생성합니다. AI는 필드명 제안에만 사용하고 전문 구조와
 * {@code GlowTrgmField.description}은 이 클래스가 원문에서 직접 추출합니다.
 */
public final class MciResponseScaffolder {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "(?m)^\\s*package\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "\\bclass\\s+([A-Za-z_$][\\w$]*)\\b");
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "(?s)((?:@[A-Za-z_$][\\w.$]*(?:\\s*\\([^;{}]*?\\))?\\s*)*)"
                    + "(?:private|protected|public)\\s+"
                    + "(?:(?:static|final|transient|volatile)\\s+)*"
                    + "([A-Za-z_$][\\w.$]*(?:\\s*<[^;{}=]+>)?(?:\\s*\\[\\])?)\\s+"
                    + "([A-Za-z_$][\\w$]*)\\s*(?:=[^;{}]*)?;");
    private static final Pattern ORDER_PATTERN = Pattern.compile("\\border\\s*=\\s*(\\d+)");
    private static final Pattern LENGTH_PATTERN = Pattern.compile("\\blength\\s*=\\s*(\\d+)");
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile(
            "\\bdescription\\s*=\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern JAVA_NAME_PATTERN = Pattern.compile("^[a-z][A-Za-z0-9]*$");
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile(
            "^[a-z_][a-z0-9_]*(?:\\.[a-z_][a-z0-9_]*)*$");
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("^[A-Z][A-Za-z0-9]*$");
    private static final List<String> SENSITIVE_KEYWORDS = List.of(
            "주민등록", "주민번호", "외국인등록", "여권번호", "계좌번호", "카드번호",
            "비밀번호", "암호", "resident registration", "social security", "passport", "password");

    private MciResponseScaffolder() {
    }

    public record ParsedSource(String packageName, String rootClassName, List<ParsedType> types) {
    }

    public record ParsedType(String name, String parentName, List<ParsedField> fields) {
    }

    public record ParsedField(String ownerType, int order, int length, String type, String name,
                              String description, boolean sensitive) {
    }

    public record FieldMapping(String ownerType, String sourceName, String targetName, boolean include) {
    }

    public record GeneratedSources(String responseSource, String converterSource) {
    }

    public record GeneratedRequestSources(String requestSource, String converterSource) {
    }

    public static ParsedSource parse(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("MCI 응답 Java 소스를 입력해주세요.");
        }
        String normalized = source.replace("\uFEFF", "");
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(normalized);
        if (!packageMatcher.find()) {
            throw new IllegalArgumentException("package 선언을 찾을 수 없습니다.");
        }

        List<ClassRange> ranges = findClassRanges(normalized);
        if (ranges.isEmpty()) {
            throw new IllegalArgumentException("Java class 선언을 찾을 수 없습니다.");
        }
        ClassRange root = ranges.stream()
                .filter(range -> range.parentName() == null && range.name().endsWith("_O"))
                .findFirst()
                .orElseGet(() -> ranges.stream().filter(range -> range.parentName() == null)
                        .findFirst().orElse(ranges.getFirst()));

        List<ParsedType> types = new ArrayList<>();
        types.add(new ParsedType(root.name(), null, parseFields(normalized, root)));
        ranges.stream()
                .filter(range -> range != root && isDescendantOf(range, root, ranges))
                .sorted(Comparator.comparingInt(ClassRange::openBrace))
                .forEach(range -> types.add(new ParsedType(
                        range.name(), range.parentName(), parseFields(normalized, range))));

        if (types.stream().allMatch(type -> type.fields().isEmpty())) {
            throw new IllegalArgumentException("변환할 응답 필드를 찾을 수 없습니다.");
        }
        return new ParsedSource(packageMatcher.group(1), root.name(), List.copyOf(types));
    }

    public static GeneratedSources generate(ParsedSource parsed, String responsePackage,
                                            String responseClassName, String converterPackage,
                                            String converterClassName, List<FieldMapping> mappings) {
        if (parsed == null || parsed.types() == null || parsed.types().isEmpty()) {
            throw new IllegalArgumentException("분석된 MCI 응답 정보가 없습니다.");
        }
        validatePackage(responsePackage, "Response package");
        validatePackage(converterPackage, "Converter package");
        validateClassName(responseClassName, "Response class");
        validateClassName(converterClassName, "Converter class");

        Map<String, FieldMapping> mappingBySource = normalizeMappings(parsed, mappings);
        validateTargetNames(parsed, mappingBySource);
        return new GeneratedSources(
                responseSource(parsed, responsePackage, responseClassName, mappingBySource),
                converterSource(parsed, responsePackage, responseClassName, converterPackage,
                        converterClassName, mappingBySource));
    }

    /**
     * LLM Request DTO에서 Glow MCI 입력 전문으로 변환하는 MapStruct Converter를 생성합니다.
     */
    public static GeneratedRequestSources generateRequest(ParsedSource parsed, String requestPackage,
                                                          String requestClassName, String converterPackage,
                                                          String converterClassName, List<FieldMapping> mappings) {
        if (parsed == null || parsed.types() == null || parsed.types().isEmpty()) {
            throw new IllegalArgumentException("분석된 MCI 요청 정보가 없습니다.");
        }
        validatePackage(requestPackage, "Request package");
        validatePackage(converterPackage, "Converter package");
        validateClassName(requestClassName, "Request class");
        validateClassName(converterClassName, "Converter class");

        Map<String, FieldMapping> mappingBySource = normalizeMappings(parsed, mappings);
        validateTargetNames(parsed, mappingBySource);
        return new GeneratedRequestSources(
                responseSource(parsed, requestPackage, requestClassName, mappingBySource, true),
                requestConverterSource(parsed, requestPackage, requestClassName, converterPackage,
                        converterClassName, mappingBySource));
    }

    private static List<ClassRange> findClassRanges(String source) {
        List<ClassRangeDraft> drafts = new ArrayList<>();
        Matcher matcher = CLASS_PATTERN.matcher(source);
        while (matcher.find()) {
            int openBrace = source.indexOf('{', matcher.end());
            if (openBrace < 0) {
                continue;
            }
            int closeBrace = matchingBrace(source, openBrace);
            if (closeBrace > openBrace) {
                drafts.add(new ClassRangeDraft(matcher.group(1), openBrace, closeBrace));
            }
        }
        drafts.sort(Comparator.comparingInt(ClassRangeDraft::openBrace));
        List<ClassRange> ranges = new ArrayList<>();
        for (ClassRangeDraft draft : drafts) {
            String parent = drafts.stream()
                    .filter(candidate -> candidate != draft
                            && candidate.openBrace() < draft.openBrace()
                            && candidate.closeBrace() > draft.closeBrace())
                    .min(Comparator.comparingInt(candidate -> candidate.closeBrace() - candidate.openBrace()))
                    .map(ClassRangeDraft::name)
                    .orElse(null);
            ranges.add(new ClassRange(draft.name(), parent, draft.openBrace(), draft.closeBrace()));
        }
        return ranges;
    }

    private static boolean isDescendantOf(ClassRange range, ClassRange root, List<ClassRange> ranges) {
        if (range.openBrace() <= root.openBrace() || range.closeBrace() >= root.closeBrace()) {
            return false;
        }
        String parent = range.parentName();
        while (parent != null) {
            if (parent.equals(root.name())) {
                return true;
            }
            String current = parent;
            parent = ranges.stream().filter(candidate -> candidate.name().equals(current))
                    .map(ClassRange::parentName).findFirst().orElse(null);
        }
        return false;
    }

    private static List<ParsedField> parseFields(String source, ClassRange range) {
        String body = source.substring(range.openBrace() + 1, range.closeBrace());
        Matcher matcher = FIELD_PATTERN.matcher(body);
        List<FieldWithPosition> fields = new ArrayList<>();
        while (matcher.find()) {
            int absoluteStart = range.openBrace() + 1 + matcher.start();
            if (braceDepth(source, range.openBrace() + 1, absoluteStart) != 0) {
                continue;
            }
            String annotations = matcher.group(1) == null ? "" : matcher.group(1);
            String type = matcher.group(2).replaceAll("\\s+", "");
            String name = matcher.group(3);
            int order = intAttribute(annotations, ORDER_PATTERN, Integer.MAX_VALUE);
            int length = intAttribute(annotations, LENGTH_PATTERN, 0);
            String description = stringAttribute(annotations, DESCRIPTION_PATTERN);
            fields.add(new FieldWithPosition(absoluteStart, new ParsedField(
                    range.name(), order, length, type, name, description, isSensitive(name, description))));
        }
        fields.sort(Comparator
                .comparingInt((FieldWithPosition value) -> value.field().order())
                .thenComparingInt(FieldWithPosition::position));
        return fields.stream().map(FieldWithPosition::field).toList();
    }

    private static int intAttribute(String source, Pattern pattern, int defaultValue) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : defaultValue;
    }

    private static String stringAttribute(String source, Pattern pattern) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? unescapeJavaString(matcher.group(1)) : "";
    }

    private static String unescapeJavaString(String value) {
        return value.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private static boolean isSensitive(String name, String description) {
        String text = (name + " " + description).toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYWORDS.stream().anyMatch(text::contains);
    }

    private static int matchingBrace(String source, int openBrace) {
        int depth = 0;
        ScanState state = ScanState.CODE;
        for (int i = openBrace; i < source.length(); i++) {
            char current = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (state == ScanState.CODE) {
                if (current == '/' && next == '/') { state = ScanState.LINE_COMMENT; i++; continue; }
                if (current == '/' && next == '*') { state = ScanState.BLOCK_COMMENT; i++; continue; }
                if (current == '"') { state = ScanState.STRING; continue; }
                if (current == '\'') { state = ScanState.CHAR; continue; }
                if (current == '{') depth++;
                if (current == '}' && --depth == 0) return i;
            } else if (state == ScanState.LINE_COMMENT && (current == '\n' || current == '\r')) {
                state = ScanState.CODE;
            } else if (state == ScanState.BLOCK_COMMENT && current == '*' && next == '/') {
                state = ScanState.CODE; i++;
            } else if ((state == ScanState.STRING || state == ScanState.CHAR) && current == '\\') {
                i++;
            } else if (state == ScanState.STRING && current == '"') {
                state = ScanState.CODE;
            } else if (state == ScanState.CHAR && current == '\'') {
                state = ScanState.CODE;
            }
        }
        return -1;
    }

    private static int braceDepth(String source, int from, int to) {
        int depth = 0;
        ScanState state = ScanState.CODE;
        for (int i = from; i < to; i++) {
            char current = source.charAt(i);
            char next = i + 1 < to ? source.charAt(i + 1) : '\0';
            if (state == ScanState.CODE) {
                if (current == '/' && next == '/') { state = ScanState.LINE_COMMENT; i++; continue; }
                if (current == '/' && next == '*') { state = ScanState.BLOCK_COMMENT; i++; continue; }
                if (current == '"') { state = ScanState.STRING; continue; }
                if (current == '\'') { state = ScanState.CHAR; continue; }
                if (current == '{') depth++;
                if (current == '}') depth--;
            } else if (state == ScanState.LINE_COMMENT && (current == '\n' || current == '\r')) {
                state = ScanState.CODE;
            } else if (state == ScanState.BLOCK_COMMENT && current == '*' && next == '/') {
                state = ScanState.CODE; i++;
            } else if ((state == ScanState.STRING || state == ScanState.CHAR) && current == '\\') {
                i++;
            } else if (state == ScanState.STRING && current == '"') {
                state = ScanState.CODE;
            } else if (state == ScanState.CHAR && current == '\'') {
                state = ScanState.CODE;
            }
        }
        return depth;
    }

    private static Map<String, FieldMapping> normalizeMappings(ParsedSource parsed, List<FieldMapping> mappings) {
        Map<String, FieldMapping> normalized = new LinkedHashMap<>();
        if (mappings != null) {
            for (FieldMapping mapping : mappings) {
                if (mapping == null || mapping.ownerType() == null || mapping.sourceName() == null) continue;
                normalized.put(key(mapping.ownerType(), mapping.sourceName()), mapping);
            }
        }
        for (ParsedType type : parsed.types()) {
            for (ParsedField field : type.fields()) {
                normalized.putIfAbsent(key(type.name(), field.name()),
                        new FieldMapping(type.name(), field.name(), field.name(), true));
            }
        }
        return normalized;
    }

    private static void validateTargetNames(ParsedSource parsed, Map<String, FieldMapping> mappings) {
        for (ParsedType type : parsed.types()) {
            Set<String> targets = new HashSet<>();
            for (ParsedField field : type.fields()) {
                FieldMapping mapping = mappings.get(key(type.name(), field.name()));
                if (!mapping.include()) continue;
                String target = mapping.targetName() == null ? "" : mapping.targetName().trim();
                if (!JAVA_NAME_PATTERN.matcher(target).matches()) {
                    throw new IllegalArgumentException("올바르지 않은 LLM 필드명: " + target);
                }
                if (!targets.add(target)) {
                    throw new IllegalArgumentException(type.name() + " 안에 중복 LLM 필드명이 있습니다: " + target);
                }
            }
        }
    }

    private static String responseSource(ParsedSource parsed, String responsePackage, String responseClassName,
                                         Map<String, FieldMapping> mappings) {
        return responseSource(parsed, responsePackage, responseClassName, mappings, false);
    }

    private static String responseSource(ParsedSource parsed, String responsePackage, String responseClassName,
                                         Map<String, FieldMapping> mappings, boolean isRequest) {
        boolean usesList = parsed.types().stream().flatMap(type -> type.fields().stream())
                .anyMatch(field -> field.type().contains("List<"));
        boolean usesBigDecimal = parsed.types().stream().flatMap(type -> type.fields().stream())
                .anyMatch(field -> field.type().contains("BigDecimal"));
        StringBuilder source = new StringBuilder("package ").append(responsePackage).append(";\n\n")
                .append("import com.fasterxml.jackson.annotation.JsonInclude;\n")
                .append("import io.swagger.v3.oas.annotations.media.Schema;\n")
                .append("import lombok.Data;\n");
        if (usesBigDecimal) source.append("import java.math.BigDecimal;\n");
        if (usesList) source.append("import java.util.List;\n");
        source.append("\n@Data\n@JsonInclude(JsonInclude.Include.NON_NULL)\n")
                .append("public class ").append(responseClassName).append(" {\n\n")
                .append(responseFields(parsed.types().getFirst(), mappings, "    ", isRequest));

        for (ParsedType type : parsed.types().stream().skip(1).toList()) {
            source.append("    @Data\n")
                    .append("    @JsonInclude(JsonInclude.Include.NON_NULL)\n")
                    .append("    public static class ").append(type.name()).append(" {\n\n")
                    .append(responseFields(type, mappings, "        ", isRequest))
                    .append("    }\n\n");
        }
        return source.append("}\n").toString();
    }



    private static String responseFields(ParsedType type, Map<String, FieldMapping> mappings, String indent) {
        return responseFields(type, mappings, indent, false);
    }

    private static String responseFields(ParsedType type, Map<String, FieldMapping> mappings, String indent, boolean isRequest) {
        StringBuilder source = new StringBuilder();
        for (ParsedField field : type.fields()) {
            FieldMapping mapping = mappings.get(key(type.name(), field.name()));
            if (!mapping.include()) continue;
            if (isRequest) {
                String example = exampleValue(field);
                source.append(indent).append("@Schema(description = \"")
                        .append(escapeJava(field.description()))
                        .append("\", example = \"").append(example).append("\")\n");
            } else {
                source.append(indent).append("@Schema(description = \"")
                        .append(escapeJava(field.description())).append("\")\n");
            }
            source.append(indent).append("private ").append(field.type()).append(' ')
                    .append(mapping.targetName().trim()).append(";\n\n");
        }
        return source.toString();
    }

    private static String exampleValue(ParsedField field) {
        String type = field.type();
        int length = field.length();
        String desc = field.description() == null ? "" : field.description();
        if (type.contains("List<") || type.contains("[]")) return "";
        if (type.equals("BigDecimal") || type.equals("Long") || type.equals("Integer") || type.equals("int") || type.equals("long")) {
            return "0";
        }
        // 날짜 패턴
        if (length == 8 && (desc.contains("일자") || desc.contains("날짜") || desc.contains("생년") || desc.contains("Ymd") || field.name().toLowerCase().contains("ymd"))) {
            return "20240101";
        }
        // 년월 패턴
        if (length == 6 && (desc.contains("년월") || desc.contains("연월") || field.name().toLowerCase().contains("ym"))) {
            return "202401";
        }
        // 코드 패턴
        if (desc.contains("코드") || desc.contains("구분") || field.name().toLowerCase().contains("cd") || field.name().toLowerCase().contains("sc")) {
            return length <= 4 ? "01" : "0001";
        }
        // 번호 패턴
        if (desc.contains("번호") || field.name().toLowerCase().contains("no")) {
            return length <= 10 ? "1234567890".substring(0, Math.min(length, 10)) : "12345678901234";
        }
        // 이름 패턴
        if (desc.contains("이름") || desc.contains("명") || field.name().toLowerCase().contains("nm")) {
            return "홍길동";
        }
        // 기본 String
        return "";
    }



    private static String converterSource(ParsedSource parsed, String responsePackage, String responseClassName,
                                          String converterPackage, String converterClassName,
                                          Map<String, FieldMapping> mappings) {
        StringBuilder source = new StringBuilder("package ").append(converterPackage).append(";\n\n")
                .append("import ").append(responsePackage).append('.').append(responseClassName).append(";\n")
                .append("import ").append(parsed.packageName()).append('.').append(parsed.rootClassName()).append(";\n")
                .append("import org.mapstruct.Mapper;\n")
                .append("import org.mapstruct.Mapping;\n")
                .append("import org.mapstruct.ReportingPolicy;\n\n")
                .append("@Mapper(componentModel = \"spring\", unmappedTargetPolicy = ReportingPolicy.IGNORE)\n")
                .append("public interface ").append(converterClassName).append(" {\n\n");

        appendMappingMethod(source, parsed.types().getFirst(), responseClassName,
                parsed.rootClassName(), "toResponse", mappings);
        Map<String, ParsedType> typeByName = new HashMap<>();
        parsed.types().forEach(type -> typeByName.put(type.name(), type));
        for (ParsedType type : parsed.types().stream().skip(1).toList()) {
            appendMappingMethod(source, type, responseClassName + "." + type.name(),
                    sourceTypePath(parsed.rootClassName(), type, typeByName), "to" + type.name(), mappings);
        }
        return source.append("}\n").toString();
    }

    private static String requestConverterSource(ParsedSource parsed, String requestPackage, String requestClassName,
                                                 String converterPackage, String converterClassName,
                                                 Map<String, FieldMapping> mappings) {
        StringBuilder source = new StringBuilder("package ").append(converterPackage).append(";\n\n")
                .append("import ").append(requestPackage).append('.').append(requestClassName).append(";\n")
                .append("import ").append(parsed.packageName()).append('.').append(parsed.rootClassName()).append(";\n")
                .append("import org.mapstruct.Mapper;\n")
                .append("import org.mapstruct.Mapping;\n")
                .append("import org.mapstruct.ReportingPolicy;\n\n")
                .append("@Mapper(componentModel = \"spring\", unmappedTargetPolicy = ReportingPolicy.IGNORE)\n")
                .append("public interface ").append(converterClassName).append(" {\n\n");

        appendRequestMappingMethod(source, parsed.types().getFirst(), parsed.rootClassName(),
                requestClassName, "toRequest", mappings);
        Map<String, ParsedType> typeByName = new HashMap<>();
        parsed.types().forEach(type -> typeByName.put(type.name(), type));
        for (ParsedType type : parsed.types().stream().skip(1).toList()) {
            appendRequestMappingMethod(source, type,
                    sourceTypePath(parsed.rootClassName(), type, typeByName),
                    requestClassName + "." + type.name(), "to" + type.name(), mappings);
        }
        return source.append("}\n").toString();
    }

    private static void appendMappingMethod(StringBuilder source, ParsedType type, String targetType,
                                            String sourceType, String methodName,
                                            Map<String, FieldMapping> mappings) {
        for (ParsedField field : type.fields()) {
            FieldMapping mapping = mappings.get(key(type.name(), field.name()));
            if (mapping.include() && !field.name().equals(mapping.targetName())) {
                source.append("    @Mapping(source = \"").append(field.name())
                        .append("\", target = \"").append(mapping.targetName()).append("\")\n");
            }
        }
        source.append("    ").append(targetType).append(' ').append(methodName)
                .append('(').append(sourceType).append(" source);\n\n");
    }

    private static void appendRequestMappingMethod(StringBuilder source, ParsedType type, String legacyTargetType,
                                                   String requestSourceType, String methodName,
                                                   Map<String, FieldMapping> mappings) {
        for (ParsedField field : type.fields()) {
            FieldMapping mapping = mappings.get(key(type.name(), field.name()));
            if (mapping.include() && !field.name().equals(mapping.targetName())) {
                source.append("    @Mapping(source = \"").append(mapping.targetName())
                        .append("\", target = \"").append(field.name()).append("\")\n");
            }
        }
        source.append("    ").append(legacyTargetType).append(' ').append(methodName)
                .append('(').append(requestSourceType).append(" source);\n\n");
    }

    private static String sourceTypePath(String rootClassName, ParsedType type,
                                         Map<String, ParsedType> typeByName) {
        List<String> names = new ArrayList<>();
        ParsedType current = type;
        while (current != null && !current.name().equals(rootClassName)) {
            names.addFirst(current.name());
            current = current.parentName() == null ? null : typeByName.get(current.parentName());
        }
        return rootClassName + "." + String.join(".", names);
    }

    private static void validatePackage(String value, String label) {
        if (value == null || !PACKAGE_NAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(label + " 이름이 올바르지 않습니다: " + value);
        }
    }

    private static void validateClassName(String value, String label) {
        if (value == null || !CLASS_NAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(label + " 이름이 올바르지 않습니다: " + value);
        }
    }

    private static String key(String ownerType, String sourceName) {
        return ownerType + "#" + sourceName;
    }

    private static String escapeJava(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    private enum ScanState { CODE, STRING, CHAR, LINE_COMMENT, BLOCK_COMMENT }

    private record ClassRangeDraft(String name, int openBrace, int closeBrace) { }
    private record ClassRange(String name, String parentName, int openBrace, int closeBrace) { }
    private record FieldWithPosition(int position, ParsedField field) { }
}
