package io.shinhanlife.dat.lib.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.shinhanlife.dat.lib.metadata.ToolDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.List;

/**
 * CI/CD 및 로컬 개발 환경에서 V17 Tool YAML 정의 파일을 읽어 Java @McpTool 어노테이션을 동기화합니다.
 */
public final class ToolAnnotationSyncRunner {

    private ToolAnnotationSyncRunner() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ToolAnnotationSyncRunner <project-root>");
        }
        sync(Path.of(args[0]));
    }

    static void sync(Path projectRoot) {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        int syncCount = 0;
        try (Stream<Path> files = Files.walk(projectRoot)) {
            List<Path> yamlFiles = files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().replace('\\', '/').contains("/src/main/resources/tool-definitions/"))
                    .filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".yaml"))
                    .toList();

            for (Path file : yamlFiles) {
                ToolDefinition definition = yamlMapper.readValue(file.toFile(), ToolDefinition.class);
                
                String toolName = definition.name();
                String title = definition.displayName();
                String description = definition.displayDescription();
                boolean readOnlyHint = Boolean.TRUE.equals(definition.readOnly());
                boolean destructiveHint = Boolean.TRUE.equals(definition.destructive());
                boolean idempotentHint = Boolean.TRUE.equals(definition.idempotent());
                String categoryKey = definition.categoryKey();

                try {
                    boolean updated = ToolSourceUpdater.syncToolSource(projectRoot, toolName, title, description,
                            readOnlyHint, destructiveHint, idempotentHint, categoryKey);
                    if (updated) {
                        syncCount++;
                        System.out.println("Synced (Updated): " + toolName);
                    } else {
                        System.out.println("Synced (Unchanged): " + toolName);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipped (Not Found): " + toolName);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to run tool annotation sync", exception);
        }
        System.out.println("Tool annotation sync completed: " + syncCount + " tools updated.");
    }
}
