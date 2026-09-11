package io.shinhanlife.dat.lib.validation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @package io.shinhanlife.dat.lib.validation
 * @className McpToolNameValidator
 * @description Validates unique MCP SDK tool names across tool modules
 * @author 0986406
 * @create 2026.07.27
 * <pre>
 * ---------- revision history ----------
 * date       author    description
 * ---------- --------- ---------------------------
 * 2026.07.27 0986406    initial creation
 * </pre>
 */
public final class McpToolNameValidator {

    private static final Pattern TOOL_NAME_PATTERN = Pattern.compile("\\bname\\s*=\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern TOOL_NAME_CONVENTION = Pattern.compile("^[a-zA-Z0-9_-]{1,128}$");

    private McpToolNameValidator() {
    }

    public static void assertUnique(Path projectRoot) {
        Map<String, List<ToolDeclaration>> declarationsByName = new LinkedHashMap<>();

        try (var modules = Files.list(projectRoot)) {
            modules.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("dat-was-"))
                    .filter(path -> !path.getFileName().toString().equals("dat-was-lib"))
                    .sorted()
                    .forEach(module -> collectDeclarations(module, declarationsByName));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to scan MCP tool modules", exception);
        }

        List<Map.Entry<String, List<ToolDeclaration>>> invalidNames = declarationsByName.entrySet().stream()
                .filter(entry -> !TOOL_NAME_CONVENTION.matcher(entry.getKey()).matches())
                .sorted(Map.Entry.comparingByKey())
                .toList();

        if (!invalidNames.isEmpty()) {
            throw new IllegalStateException(buildInvalidNameMessage(invalidNames));
        }
        List<Map.Entry<String, List<ToolDeclaration>>> duplicates = declarationsByName.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .sorted(Map.Entry.comparingByKey())
                .toList();

        if (!duplicates.isEmpty()) {
            throw new IllegalStateException(buildDuplicateMessage(duplicates));
        }
    }

    private static void collectDeclarations(Path module, Map<String, List<ToolDeclaration>> declarationsByName) {
        Path sourceDirectory = module.resolve("src/main/java");
        if (!Files.isDirectory(sourceDirectory)) {
            return;
        }

        try (var sources = Files.walk(sourceDirectory)) {
            sources.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .forEach(source -> collectDeclarations(module.getFileName().toString(), source, declarationsByName));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to scan module " + module.getFileName(), exception);
        }
    }

    private static void collectDeclarations(String moduleName, Path source,
                                            Map<String, List<ToolDeclaration>> declarationsByName) {
        String content;
        try {
            content = Files.readString(source);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read " + source, exception);
        }

        int annotationOffset = content.indexOf("@McpTool");
        while (annotationOffset >= 0) {
            int openingParenthesis = content.indexOf('(', annotationOffset);
            int closingParenthesis = findAnnotationEnd(content, openingParenthesis);
            if (openingParenthesis < 0 || closingParenthesis < 0) {
                break;
            }

            Matcher matcher = TOOL_NAME_PATTERN.matcher(content.substring(openingParenthesis + 1, closingParenthesis));
            if (matcher.find()) {
                String toolName = matcher.group(1);
                int line = 1 + (int) content.substring(0, annotationOffset).chars().filter(character -> character == '\n').count();
                declarationsByName.computeIfAbsent(toolName, ignored -> new ArrayList<>())
                        .add(new ToolDeclaration(moduleName, source, line));
            }
            annotationOffset = content.indexOf("@McpTool", closingParenthesis + 1);
        }
    }

    private static int findAnnotationEnd(String content, int openingParenthesis) {
        if (openingParenthesis < 0) {
            return -1;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = openingParenthesis; index < content.length(); index++) {
            char character = content.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '\"') {
                    inString = false;
                }
                continue;
            }
            if (character == '\"') {
                inString = true;
            } else if (character == '(') {
                depth++;
            } else if (character == ')' && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static String buildInvalidNameMessage(List<Map.Entry<String, List<ToolDeclaration>>> invalidNames) {
        StringBuilder message = new StringBuilder("Invalid MCP tool name(s): expected 1-128 characters using letters, digits, underscores, or hyphens.");
        for (Map.Entry<String, List<ToolDeclaration>> invalidName : invalidNames) {
            message.append("\n\n").append(invalidName.getKey());
            invalidName.getValue().stream()
                    .sorted(Comparator.comparing(ToolDeclaration::moduleName).thenComparing(declaration -> declaration.source().toString()))
                    .forEach(declaration -> message.append("\n- ")
                            .append(declaration.moduleName())
                            .append(": ")
                            .append(declaration.source())
                            .append(':').append(declaration.line()));
        }
        return message.toString();
    }

    private static String buildDuplicateMessage(List<Map.Entry<String, List<ToolDeclaration>>> duplicates) {
        StringBuilder message = new StringBuilder("Duplicate MCP tool name(s):");
        for (Map.Entry<String, List<ToolDeclaration>> duplicate : duplicates) {
            message.append("\n\n").append(duplicate.getKey());
            duplicate.getValue().stream()
                    .sorted(Comparator.comparing(ToolDeclaration::moduleName).thenComparing(declaration -> declaration.source().toString()))
                    .forEach(declaration -> message.append("\n- ")
                            .append(declaration.moduleName())
                            .append(": ")
                            .append(declaration.source())
                            .append(':').append(declaration.line()));
        }
        return message.toString();
    }

    private record ToolDeclaration(String moduleName, Path source, int line) {
    }
}
