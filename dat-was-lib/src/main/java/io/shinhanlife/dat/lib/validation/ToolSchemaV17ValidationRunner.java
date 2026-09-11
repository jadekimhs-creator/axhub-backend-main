package io.shinhanlife.dat.lib.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.shinhanlife.dat.lib.metadata.ToolDefinition;
import io.shinhanlife.dat.lib.metadata.ToolDefinitionValidator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** CI/CD에서 모든 Java MCP Tool과 V17 정의 파일의 1:1 대응을 검증합니다. */
public final class ToolSchemaV17ValidationRunner {
    private static final Pattern MCP_TOOL_NAME = Pattern.compile(
            "@McpTool\\s*\\(\\s*name\\s*=\\s*\"([^\"]+)\"");

    private ToolSchemaV17ValidationRunner() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ToolSchemaV17ValidationRunner <project-root>");
        }
        validate(Path.of(args[0]));
    }

    static void validate(Path projectRoot) {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        Map<String, Path> definitions = new LinkedHashMap<>();
        Set<String> toolNames = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(projectRoot)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String normalized = file.toString().replace('\\', '/');
                if (normalized.contains("/build/") || normalized.contains("/.gradle/")
                        || normalized.contains("/tmp_") || normalized.contains("/org/")) {
                    continue;
                }
                if (normalized.endsWith(".java") && normalized.contains("/dat-was-")
                        && !normalized.contains("/dat-was-lib/")) {
                    Matcher matcher = MCP_TOOL_NAME.matcher(Files.readString(file, StandardCharsets.UTF_8));
                    while (matcher.find()) {
                        toolNames.add(matcher.group(1));
                    }
                }
                if (normalized.contains("/src/main/resources/tool-definitions/")
                        && (normalized.endsWith(".yml") || normalized.endsWith(".yaml"))) {
                    ToolDefinition definition = yamlMapper.readValue(file.toFile(), ToolDefinition.class);
                    ToolDefinitionValidator.validate(definition, file.toString());
                    Path previous = definitions.putIfAbsent(definition.name(), file);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate V17 Tool definition: " + definition.name()
                                + " [" + previous + ", " + file + "]");
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan Tool schema V17 files", exception);
        }
        Set<String> missing = new LinkedHashSet<>(toolNames);
        missing.removeAll(definitions.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing V17 Tool definitions: " + missing);
        }
        Set<String> orphan = new LinkedHashSet<>(definitions.keySet());
        orphan.removeAll(toolNames);
        if (!orphan.isEmpty()) {
            throw new IllegalStateException("V17 definitions without matching @McpTool: " + orphan);
        }
        System.out.println("Tool schema V17 validation passed: " + toolNames.size() + " tools");
    }
}
