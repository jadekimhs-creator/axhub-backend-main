package io.shinhanlife.dat.report.source;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import io.shinhanlife.dat.report.model.FieldDefinition;
import io.shinhanlife.dat.report.model.ToolReportModel;
import io.shinhanlife.dat.report.model.ToolSourceType;
import io.shinhanlife.dat.report.model.ToolSummary;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import org.springframework.stereotype.Component;

/**
 * @package io.shinhanlife.dat.report.source
 * @className ToolDetailAnalyzer
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
public class ToolDetailAnalyzer {

    private final ToolSourceDiscovery discovery;

    public ToolDetailAnalyzer(ToolSourceDiscovery discovery) {
        this.discovery = discovery;
    }

    public ToolReportModel analyze(ToolSummary tool, ToolSourceType sourceType) {
        return analyze(tool, discovery.sourceRoot(sourceType));
    }

    public ToolReportModel analyze(ToolSummary tool, Path sourceRoot) {
        List<FieldDefinition> fields = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        Map<String, Path> javaFiles = indexJavaFiles(sourceRoot);

        collectJavaType(sourceRoot, tool, javaFiles.get(tool.requestType()), "DTO", "INPUT", fields, diagnostics);
        collectJavaType(sourceRoot, tool, javaFiles.get(tool.responseType()), "DTO", "OUTPUT", fields, diagnostics);
        if (!tool.mappingId().isBlank()) {
            collectJavaType(sourceRoot, tool, javaFiles.get(tool.mappingId() + "_I"),
                    "TELEGRAM", "INPUT", fields, diagnostics);
            collectJavaType(sourceRoot, tool, javaFiles.get(tool.mappingId() + "_O"),
                    "TELEGRAM", "OUTPUT", fields, diagnostics);
        }
        return new ToolReportModel(tool, List.copyOf(fields), List.copyOf(diagnostics));
    }

    private Map<String, Path> indexJavaFiles(Path sourceRoot) {
        Map<String, Path> result = new LinkedHashMap<>();
        if (sourceRoot == null || !Files.isDirectory(sourceRoot)) {
            return result;
        }
        indexDirectory(sourceRoot, result);
        Path parent = sourceRoot.getParent();
        if (parent != null) {
            Path libmt = parent.resolve("dat-lib-datmt");
            if (Files.isDirectory(libmt) && !libmt.equals(sourceRoot)) {
                indexDirectory(libmt, result);
            }
        }
        return result;
    }

    private void indexDirectory(Path dir, Map<String, Path> result) {
        try (var paths = Files.walk(dir)) {
            paths.filter(this::isAnalyzableJavaSource)
                    .sorted(Comparator.comparing((Path p) -> p.toString().replace('\\', '/').contains("/src/main/java/") ? 0 : 1))
                    .forEach(path -> result.putIfAbsent(path.getFileName().toString().replace(".java", ""), path));
        } catch (IOException exception) {
            // Ignore directory indexing errors for fallback
        }
    }

    private boolean isAnalyzableJavaSource(Path path) {
        if (!Files.isRegularFile(path) || !path.getFileName().toString().endsWith(".java")) {
            return false;
        }
        String normalized = path.toString().replace('\\', '/');
        return !normalized.contains("/.gradle/")
                && !normalized.contains("/.git/")
                && !normalized.contains("/.metadata/")
                && !normalized.contains("/build/classes/")
                && !normalized.contains("/build/reports/")
                && !normalized.contains("/build/test-results/")
                && !normalized.contains("/build/tmp/")
                && !normalized.contains("/bin/")
                && !normalized.contains("/out/")
                && !normalized.contains("/dap-tool-report/");
    }

    private void collectJavaType(Path sourceRoot, ToolSummary tool, Path path, String sourceKind, String direction,
                                 List<FieldDefinition> fields, List<String> diagnostics) {
        if (path == null) {
            diagnostics.add(sourceKind + " " + direction + " 타입을 찾을 수 없음");
            return;
        }
        try {
            CompilationUnit unit = StaticJavaParser.parse(path);
            for (TypeDeclaration<?> type : unit.getTypes()) {
                collectFields(sourceRoot, tool, path, type, sourceKind, direction, fields);
                type.getMembers().stream().filter(TypeDeclaration.class::isInstance)
                        .map(TypeDeclaration.class::cast)
                        .forEach(nested -> collectFields(sourceRoot, tool, path, nested, sourceKind, direction, fields));
            }
        } catch (Exception exception) {
            diagnostics.add(sourceKind + " 분석 실패: " + relative(sourceRoot, path));
        }
    }

    private void collectFields(Path sourceRoot, ToolSummary tool, Path path, TypeDeclaration<?> owner,
                               String sourceKind, String direction, List<FieldDefinition> fields) {
        if (owner.isRecordDeclaration()) {
            com.github.javaparser.ast.body.RecordDeclaration recordDecl = owner.asRecordDeclaration();
            for (com.github.javaparser.ast.body.Parameter parameter : recordDecl.getParameters()) {
                AnnotationExpr param = JavaAnnotationReader.find(parameter.getAnnotations(), "McpToolParam").orElse(null);
                AnnotationExpr schema = JavaAnnotationReader.find(parameter.getAnnotations(), "Schema").orElse(null);
                AnnotationExpr telegram = JavaAnnotationReader.find(parameter.getAnnotations(), "GlowTrgmField").orElse(null);
                fields.add(new FieldDefinition(
                        tool.name(), sourceKind, direction, owner.getNameAsString(), parameter.getNameAsString(),
                        parameter.getTypeAsString(),
                        param == null ? null : JavaAnnotationReader.bool(param, "required", false),
                        param == null ? "" : JavaAnnotationReader.string(param, "description", ""),
                        annotationConstraints(schema, telegram), relative(sourceRoot, path)));
            }
        }
        for (FieldDeclaration declaration : owner.getFields()) {
            AnnotationExpr param = JavaAnnotationReader.find(declaration.getAnnotations(), "McpToolParam").orElse(null);
            AnnotationExpr schema = JavaAnnotationReader.find(declaration.getAnnotations(), "Schema").orElse(null);
            AnnotationExpr telegram = JavaAnnotationReader.find(declaration.getAnnotations(), "GlowTrgmField").orElse(null);
            declaration.getVariables().forEach(variable -> fields.add(new FieldDefinition(
                    tool.name(), sourceKind, direction, owner.getNameAsString(), variable.getNameAsString(),
                    variable.getTypeAsString(),
                    param == null ? null : JavaAnnotationReader.bool(param, "required", false),
                    param == null ? "" : JavaAnnotationReader.string(param, "description", ""),
                    annotationConstraints(schema, telegram), relative(sourceRoot, path))));
        }
    }

    private String annotationConstraints(AnnotationExpr schema, AnnotationExpr telegram) {
        List<String> values = new ArrayList<>();
        if (schema != null) values.add("Schema=" + schema);
        if (telegram != null) values.add("GlowTrgmField=" + telegram);
        return String.join("; ", values);
    }

    private String relative(Path sourceRoot, Path path) {
        return sourceRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }
}
