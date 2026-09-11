package io.shinhanlife.dat.lib.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;

/** Updates MCP SDK and project-owned metadata in a generated tool source file. */
public final class ToolSourceUpdater {

    private ToolSourceUpdater() {
    }

    public static void updateToolSource(String toolName, String categoryKey, String description,
                                        boolean register, Boolean requiresApproval) throws IOException {
        String configuredSourceDirectory = System.getenv("AXHUB_SOURCE_DIR");
        Path rootDirectory = configuredSourceDirectory == null || configuredSourceDirectory.isBlank()
                ? Paths.get(".") : Paths.get(configuredSourceDirectory);
        updateToolSource(rootDirectory, toolName, categoryKey, description, register, requiresApproval);
    }

    static void updateToolSource(Path rootDirectory, String toolName, String categoryKey, String description,
                                 boolean register, Boolean requiresApproval) throws IOException {
        Path targetFile = findToolSource(rootDirectory, toolName);
        if (targetFile == null) {
            throw new IllegalArgumentException("Tool source not found: " + toolName);
        }

        String content = Files.readString(targetFile, StandardCharsets.UTF_8);
        content = updateMcpTool(content, toolName, description);
        content = updateToolHint(content, categoryKey, register, requiresApproval);
        Files.writeString(targetFile, content, StandardCharsets.UTF_8);
    }

    public static boolean syncToolSource(Path rootDirectory, String toolName, String title, String description,
                                         boolean readOnlyHint, boolean destructiveHint, boolean idempotentHint,
                                         String categoryKey) throws IOException {
        Path targetFile = findToolSource(rootDirectory, toolName);
        if (targetFile == null) {
            throw new IllegalArgumentException("Tool source not found: " + toolName);
        }

        String content = Files.readString(targetFile, StandardCharsets.UTF_8);
        String originalContent = content;

        AnnotationRange mcpRange = annotationArguments(content, "McpTool", toolName);
        if (mcpRange != null) {
            if (title != null) {
                content = replaceAttribute(content, mcpRange, "title", quote(title));
                mcpRange = annotationArguments(content, "McpTool", toolName);
            }
            if (description != null) {
                content = replaceAttribute(content, mcpRange, "description", quote(description));
            }
        }

        AnnotationRange hintRange = annotationArguments(content, "GrowToolHint", null);
        if (hintRange != null) {
            if (categoryKey != null && !categoryKey.isBlank()) {
                content = replaceAttribute(content, hintRange, "categoryKey", quote(categoryKey));
                hintRange = annotationArguments(content, "GrowToolHint", null);
            }
            content = replaceAttribute(content, hintRange, "destructive", Boolean.toString(destructiveHint));
            hintRange = annotationArguments(content, "GrowToolHint", null);
            content = replaceAttribute(content, hintRange, "idempotent", Boolean.toString(idempotentHint));
        }

        if (!content.equals(originalContent)) {
            Files.writeString(targetFile, content, StandardCharsets.UTF_8);
            return true;
        }
        return false;
    }

    private static Path findToolSource(Path rootDirectory, String toolName) throws IOException {
        try (var paths = Files.walk(rootDirectory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("UseCase.java"))
                    .filter(path -> path.toString().contains("dat-was-") || path.toString().contains("axhub-tool-"))
                    .sorted(Comparator.naturalOrder())
                    .filter(path -> containsMcpTool(path, toolName))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static boolean containsMcpTool(Path path, String toolName) {
        try {
            return annotationArguments(Files.readString(path, StandardCharsets.UTF_8), "McpTool", toolName) != null;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read tool source: " + path, exception);
        }
    }

    private static String updateMcpTool(String content, String toolName, String description) {
        AnnotationRange range = annotationArguments(content, "McpTool", toolName);
        if (range == null) {
            throw new IllegalArgumentException("McpTool declaration not found: " + toolName);
        }
        return description == null ? content : replaceAttribute(content, range, "description", quote(description));
    }

    private static String updateToolHint(String content, String categoryKey, boolean register, Boolean requiresApproval) {
        AnnotationRange range = annotationArguments(content, "GrowToolHint", null);
        if (range == null) {
            throw new IllegalArgumentException("GrowToolHint declaration not found next to McpTool");
        }
        String updated = replaceAttribute(content, range, "register", Boolean.toString(register));
        range = annotationArguments(updated, "GrowToolHint", null);
        if (requiresApproval != null) {
            updated = replaceAttribute(updated, range, "requiresApproval", Boolean.toString(requiresApproval));
            range = annotationArguments(updated, "GrowToolHint", null);
        }
        if (categoryKey != null && !categoryKey.isBlank()) {
            updated = replaceAttribute(updated, range, "categoryKey", quote(categoryKey));
        }
        return updated;
    }

    private static String replaceAttribute(String content, AnnotationRange range, String attribute, String value) {
        String arguments = content.substring(range.argumentsStart(), range.argumentsEnd());
        String pattern = "\\b" + attribute + "\\s*=\\s*(?:true|false|\\\"(?:\\\\.|[^\\\"\\\\])*\\\")";
        String replacement = arguments.replaceFirst(pattern, attribute + " = " + value);
        if (replacement.equals(arguments)) {
            replacement = arguments.isBlank() ? attribute + " = " + value : arguments + ", " + attribute + " = " + value;
        }
        return content.substring(0, range.argumentsStart()) + replacement + content.substring(range.argumentsEnd());
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static AnnotationRange annotationArguments(String content, String annotationName, String toolName) {
        int offset = content.indexOf("@" + annotationName);
        while (offset >= 0) {
            int openingParenthesis = content.indexOf('(', offset);
            int closingParenthesis = findAnnotationEnd(content, openingParenthesis);
            if (openingParenthesis < 0 || closingParenthesis < 0) {
                return null;
            }
            AnnotationRange range = new AnnotationRange(openingParenthesis + 1, closingParenthesis);
            if (toolName == null || content.substring(range.argumentsStart(), range.argumentsEnd())
                    .matches("(?s).*\\bname\\s*=\\s*\\\"" + java.util.regex.Pattern.quote(toolName) + "\\\".*")) {
                return range;
            }
            offset = content.indexOf("@" + annotationName, closingParenthesis + 1);
        }
        return null;
    }

    private static int findAnnotationEnd(String content, int openingParenthesis) {
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
            } else if (character == '\"') {
                inString = true;
            } else if (character == '(') {
                depth++;
            } else if (character == ')' && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

    private record AnnotationRange(int argumentsStart, int argumentsEnd) {
    }
}
