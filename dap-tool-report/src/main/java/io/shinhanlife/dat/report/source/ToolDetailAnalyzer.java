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
import org.springframework.stereotype.Component;

/** 선택한 프로젝트의 DTO와 전문 Java 어노테이션에서 Report 파라미터를 수집한다. */
@Component
public class ToolDetailAnalyzer {

    private final ToolSourceDiscovery discovery;

    public ToolDetailAnalyzer(ToolSourceDiscovery discovery) {
        this.discovery = discovery;
    }

    public ToolReportModel analyze(ToolSummary tool, ToolSourceType sourceType) {
        List<FieldDefinition> fields = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        Path sourceRoot = discovery.sourceRoot(sourceType);
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
        try (var paths = Files.walk(sourceRoot)) {
            paths.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java"))
                    .forEach(path -> result.putIfAbsent(path.getFileName().toString().replace(".java", ""), path));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to index Java sources: " + sourceRoot, exception);
        }
        return result;
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
