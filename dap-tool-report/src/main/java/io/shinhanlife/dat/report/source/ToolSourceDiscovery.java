package io.shinhanlife.dat.report.source;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import io.shinhanlife.dat.report.config.ReportProperties;
import io.shinhanlife.dat.report.model.ToolSummary;
import io.shinhanlife.dat.report.model.ToolSourceType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import org.springframework.stereotype.Component;

/** 프로젝트의 @McpTool 선언을 읽기 전용으로 탐색한다. */
@Component
public class ToolSourceDiscovery {

    private final Map<ToolSourceType, Path> sourceRoots = new EnumMap<>(ToolSourceType.class);

    public ToolSourceDiscovery(ReportProperties properties) {
        sourceRoots.put(ToolSourceType.DAP_WAS_DAPMT,
                resolveConfiguredRoot(properties.dapWasDapmtSourceRoot(), "report.dap-was-dapmt-source-root"));
        sourceRoots.put(ToolSourceType.DAP_ADMIN,
                resolveConfiguredRoot(properties.dapAdminSourceRoot(), "report.dap-admin-source-root"));
    }

    public Path sourceRoot(ToolSourceType sourceType) {
        return sourceRoots.get(sourceType);
    }

    public List<ToolSummary> discover(ToolSourceType sourceType) {
        Path sourceRoot = sourceRoot(sourceType);
        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalStateException("Report source root does not exist: " + sourceRoot);
        }
        List<ToolSummary> tools = new ArrayList<>();
        try (var paths = Files.walk(sourceRoot)) {
            paths.filter(this::isUseCaseSource).forEach(path -> parse(sourceRoot, path, tools));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan tool sources: " + sourceRoot, exception);
        }
        return tools.stream()
                .sorted(Comparator.comparing(ToolSummary::sourceFile))
                .collect(java.util.stream.Collectors.toMap(ToolSummary::name, java.util.function.Function.identity(),
                        (first, duplicate) -> first, LinkedHashMap::new)).values().stream()
                .sorted(Comparator.comparing(ToolSummary::name))
                .toList();
    }

    private Path resolveConfiguredRoot(String configuredRoot, String propertyName) {
        if (configuredRoot == null || configuredRoot.isBlank()) {
            throw new IllegalArgumentException(propertyName + " must not be blank");
        }
        return resolveProjectRoot(Path.of(configuredRoot).toAbsolutePath().normalize());
    }

    private Path resolveProjectRoot(Path configuredPath) {
        Path candidate = configuredPath;
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("settings.gradle"))
                    && Files.isDirectory(candidate.resolve("dat-was-lib"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        return configuredPath;
    }

    private boolean isUseCaseSource(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return Files.isRegularFile(path)
                && path.getFileName().toString().endsWith("UseCase.java")
                && normalized.contains("/src/main/java/")
                && !normalized.contains("/dap-tool-report/");
    }

    private void parse(Path sourceRoot, Path path, List<ToolSummary> tools) {
        try {
            CompilationUnit unit = StaticJavaParser.parse(path);
            String owner = unit.getPrimaryTypeName().orElse(path.getFileName().toString().replace(".java", ""));
            for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
                JavaAnnotationReader.find(method.getAnnotations(), "McpTool")
                        .ifPresent(annotation -> tools.add(toSummary(sourceRoot, path, owner, method, annotation)));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse tool source: " + path, exception);
        }
    }

    private ToolSummary toSummary(Path sourceRoot, Path path, String owner, MethodDeclaration method, AnnotationExpr tool) {
        AnnotationExpr hint = JavaAnnotationReader.find(method.getAnnotations(), "GrowToolHint")
                .or(() -> JavaAnnotationReader.find(method.getAnnotations(), "ToolHint")).orElse(null);
        AnnotationExpr annotations = JavaAnnotationReader.nested(tool, "annotations").orElse(null);
        String name = JavaAnnotationReader.string(tool, "name", method.getNameAsString());
        String title = JavaAnnotationReader.string(tool, "title", name);
        String description = JavaAnnotationReader.string(tool, "description", "");
        String requestType = method.getParameters().isEmpty() ? "" : method.getParameter(0).getTypeAsString();
        return new ToolSummary(
                name,
                title,
                description,
                hint == null ? "common" : JavaAnnotationReader.string(hint, "categoryKey", "com"),
                hint == null ? "" : JavaAnnotationReader.string(hint, "mappingId", ""),
                hint != null && JavaAnnotationReader.bool(hint, "register", false),
                hint != null && JavaAnnotationReader.bool(hint, "requiresApproval", false),
                annotations != null && JavaAnnotationReader.bool(annotations, "readOnlyHint", false),
                annotations != null
                        ? JavaAnnotationReader.bool(annotations, "destructiveHint",
                                hint != null && JavaAnnotationReader.bool(hint, "destructive", false))
                        : hint != null && JavaAnnotationReader.bool(hint, "destructive", false),
                annotations != null
                        ? JavaAnnotationReader.bool(annotations, "idempotentHint",
                                hint != null && JavaAnnotationReader.bool(hint, "idempotent", false))
                        : hint != null && JavaAnnotationReader.bool(hint, "idempotent", false),
                annotations != null && JavaAnnotationReader.bool(annotations, "openWorldHint", false),
                requestType,
                method.getTypeAsString(),
                owner,
                sourceRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/'),
                hint == null ? "" : JavaAnnotationReader.string(hint, "inputSchemaResource", ""),
                hint == null ? "" : JavaAnnotationReader.string(hint, "outputSchemaResource", ""),
                hint == null ? "" : JavaAnnotationReader.string(hint, "version", ""),
                hint == null ? "" : JavaAnnotationReader.string(hint, "functionDescription", ""),
                hint == null ? "" : JavaAnnotationReader.string(hint, "whenToUse", ""),
                hint == null ? "" : JavaAnnotationReader.string(hint, "whenNotToUse", ""),
                hint == null ? "" : JavaAnnotationReader.string(hint, "ioLimits", ""),
                hint == null ? "" : JavaAnnotationReader.string(hint, "displayDescription", ""),
                hint == null ? "" : JavaAnnotationReader.strings(hint, "exampleQueries"),
                hint == null ? "" : JavaAnnotationReader.strings(hint, "tags"),
                hint == null ? "" : JavaAnnotationReader.strings(hint, "requiredEnvKeys"),
                hint == null ? "" : JavaAnnotationReader.string(hint, "ownerOrg", ""), "");
    }
}
