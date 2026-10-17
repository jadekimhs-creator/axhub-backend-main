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

/**
 * @package io.shinhanlife.dat.report.source
 * @className ToolSourceDiscovery
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 * 
 * </pre>
 */
@Component
public class ToolSourceDiscovery {

    private final Map<ToolSourceType, Path> sourceRoots = new EnumMap<>(ToolSourceType.class);

    public ToolSourceDiscovery(ReportProperties properties) {
        sourceRoots.put(ToolSourceType.DAP_WAS_DAPMT,
                resolveConfiguredRoot(ToolSourceType.DAP_WAS_DAPMT, properties.dapWasDapmtSourceRoot()));
        sourceRoots.put(ToolSourceType.DAP_ADMIN,
                resolveConfiguredRoot(ToolSourceType.DAP_ADMIN, properties.dapAdminSourceRoot()));
        sourceRoots.put(ToolSourceType.DAT_WAS_DATPS,
                resolveConfiguredRoot(ToolSourceType.DAT_WAS_DATPS, properties.datWasDatpsSourceRoot()));
    }

    public Path sourceRoot(ToolSourceType sourceType) {
        return sourceRoots.get(sourceType == null ? ToolSourceType.DAP_WAS_DAPMT : sourceType);
    }

    public List<ToolSummary> discover(ToolSourceType sourceType) {
        return discover(sourceRoot(sourceType));
    }

    public List<ToolSummary> discover(Path sourceRoot) {
        if (sourceRoot == null || !Files.isDirectory(sourceRoot)) {
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

    private Path resolveConfiguredRoot(ToolSourceType type, String configuredRoot) {
        List<Path> candidates = new ArrayList<>();
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            candidates.add(Path.of(configuredRoot));
        }

        switch (type) {
            case DAP_WAS_DAPMT -> {
                candidates.add(Path.of("../dat-was-datmt"));
                candidates.add(Path.of("../../dat-was-datmt"));
                candidates.add(Path.of("../dap-was-dapmt"));
                candidates.add(Path.of("../../dap-was-dapmt"));
                candidates.add(Path.of("C:/eGovFrameDev-4.3.1-64bit/workspace-egov/dat-was-datmt"));
                candidates.add(Path.of("C:/eGovFrameDev-4.3.1-64bit/workspace-egov/dap-was-dapmt"));
            }
            case DAT_WAS_DATPS -> {
                candidates.add(Path.of("../../workspace/dat-was-datps"));
                candidates.add(Path.of("../workspace/dat-was-datps"));
                candidates.add(Path.of("../dat-was-datps"));
                candidates.add(Path.of("../../dat-was-datps"));
                candidates.add(Path.of("C:/eGovFrameDev-4.3.1-64bit/workspace/dat-was-datps"));
            }
            case DAP_ADMIN -> {
                candidates.add(Path.of("."));
                candidates.add(Path.of(".."));
                candidates.add(Path.of("C:/eGovFrameDev-4.3.1-64bit/workspace-egov/axhub-backend-main"));
            }
        }

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized)) {
                Path resolved = resolveProjectRoot(normalized, type);
                if (resolved != null && Files.isDirectory(resolved)) {
                    return resolved;
                }
            }
        }

        return candidates.isEmpty() ? Path.of(".").toAbsolutePath().normalize()
                : candidates.get(0).toAbsolutePath().normalize();
    }

    private Path resolveProjectRoot(Path configuredPath, ToolSourceType type) {
        Path candidate = configuredPath;
        while (candidate != null) {
            if (isProjectRootForType(candidate, type)) {
                return candidate;
            }
            Path parent = candidate.getParent();
            if (parent == null || parent.equals(candidate)) break;
            if (candidate.getFileName() != null && candidate.getFileName().toString().startsWith("workspace")) {
                break;
            }
            candidate = parent;
        }
        return configuredPath;
    }

    private boolean isProjectRootForType(Path path, ToolSourceType type) {
        if (!Files.isDirectory(path)) return false;
        return switch (type) {
            case DAP_WAS_DAPMT -> Files.isDirectory(path.resolve("dat-was-cus"))
                    || (Files.isRegularFile(path.resolve("settings.gradle")) && Files.isDirectory(path.resolve("dat-was-lib")) && !Files.isDirectory(path.resolve("dat-gateway")));
            case DAT_WAS_DATPS -> Files.isRegularFile(path.resolve("build.gradle")) && Files.isDirectory(path.resolve("src"));
            case DAP_ADMIN -> Files.isDirectory(path.resolve("dat-gateway"))
                    || (Files.isRegularFile(path.resolve("settings.gradle")) && Files.isDirectory(path.resolve("dat-gateway")));
        };
    }

    private boolean isUseCaseSource(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return Files.isRegularFile(path)
                && path.getFileName().toString().endsWith("UseCase.java")
                && normalized.contains("/src/main/java/")
                && !normalized.contains("/build/classes/")
                && !normalized.contains("/build/reports/")
                && !normalized.contains("/build/test-results/")
                && !normalized.contains("/build/tmp/")
                && !normalized.contains("/.gradle/")
                && !normalized.contains("/bin/")
                && !normalized.contains("/dap-tool-report/")
                && !normalized.contains("/dat-lib-datmt/");
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
            System.err.println("Warning: failed to parse tool source: " + path + " - " + exception.getMessage());
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
