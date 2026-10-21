package io.shinhanlife.dat.report.source;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
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
    private final JavaParser javaParser = new JavaParser(
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));

    private final Map<Path, Map<String, Path>> indexedRootsCache = new java.util.concurrent.ConcurrentHashMap<>();

    public ToolDetailAnalyzer(ToolSourceDiscovery discovery) {
        this.discovery = discovery;
    }

    public ToolReportModel analyze(ToolSummary tool, ToolSourceType sourceType) {
        return analyze(tool, discovery.sourceRoot(sourceType));
    }

    public ToolReportModel analyze(ToolSummary tool, Path sourceRoot) {
        return analyze(tool, sourceRoot, indexJavaFiles(sourceRoot));
    }

    public ToolReportModel analyze(ToolSummary tool, Path sourceRoot, Map<String, Path> javaFiles) {
        List<FieldDefinition> fields = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        Map<String, Path> resolvedFiles = (javaFiles != null && !javaFiles.isEmpty()) ? javaFiles : indexJavaFiles(sourceRoot);

        collectJavaType(sourceRoot, tool, resolvedFiles.get(tool.requestType()), "DTO", "INPUT", fields, diagnostics);
        collectJavaType(sourceRoot, tool, resolvedFiles.get(tool.responseType()), "DTO", "OUTPUT", fields, diagnostics);

        String telegramServiceId = resolveTelegramServiceId(tool, resolvedFiles);
        if (telegramServiceId != null && !telegramServiceId.isBlank()) {
            collectJavaType(sourceRoot, tool, resolvedFiles.get(telegramServiceId + "_I"),
                    "TELEGRAM", "INPUT", fields, diagnostics);
            collectJavaType(sourceRoot, tool, resolvedFiles.get(telegramServiceId + "_O"),
                    "TELEGRAM", "OUTPUT", fields, diagnostics);
        } else if (!tool.mappingId().isBlank()) {
            collectJavaType(sourceRoot, tool, resolvedFiles.get(tool.mappingId() + "_I"),
                    "TELEGRAM", "INPUT", fields, diagnostics);
            collectJavaType(sourceRoot, tool, resolvedFiles.get(tool.mappingId() + "_O"),
                    "TELEGRAM", "OUTPUT", fields, diagnostics);
        }
        return new ToolReportModel(tool, List.copyOf(fields), List.copyOf(diagnostics));
    }

    private String resolveTelegramServiceId(ToolSummary tool, Map<String, Path> javaFiles) {
        String mappingId = tool.mappingId() != null ? tool.mappingId().trim() : "";
        if (mappingId.matches("^ON[A-Z0-9]{6}$")) {
            return mappingId;
        }

        // 인사/직원 관련 툴인 경우 ONBSA1030 우선
        if ("DATNBSO00004".equalsIgnoreCase(mappingId)
                || tool.name().contains("employee")
                || tool.name().contains("praf")
                || (tool.title() != null && (tool.title().contains("인사") || tool.title().contains("직원")))
                || (tool.description() != null && (tool.description().contains("인사검색") || tool.description().contains("ONBSA1030")))) {
            if (javaFiles.containsKey("ONBSA1030_I") || javaFiles.containsKey("ONBSA1030_O")) {
                return "ONBSA1030";
            }
        }

        Map<String, String> ifToService = Map.of(
                "DATNBSO00004", "ONBSA1030",
                "DATNBSO00005", "ONBSZ0460",
                "DATNBTO00001", "ONBTI2370",
                "DATNCSO00001", "ONCSC1340",
                "CLYMCI00001", "ONCST1341",
                "CLHTTP00005", "ONCSD1340"
        );
        if (ifToService.containsKey(mappingId)) {
            String srv = ifToService.get(mappingId);
            if (javaFiles.containsKey(srv + "_I") || javaFiles.containsKey(srv + "_O")) {
                return srv;
            }
        }

        return null;
    }

    public Map<String, Path> indexJavaFiles(Path sourceRoot) {
        if (sourceRoot == null || !Files.isDirectory(sourceRoot)) {
            return Map.of();
        }
        return indexedRootsCache.computeIfAbsent(sourceRoot.toAbsolutePath().normalize(), this::doIndexJavaFiles);
    }

    private Map<String, Path> doIndexJavaFiles(Path sourceRoot) {
        Map<String, Path> result = new LinkedHashMap<>();
        indexDirectory(sourceRoot, result);
        Path parent = sourceRoot.getParent();
        if (parent != null) {
            Path libmt = parent.resolve("dat-lib-datmt");
            if (Files.isDirectory(libmt) && !libmt.equals(sourceRoot)) {
                indexDirectory(libmt, result);
            }
            Path datps = parent.resolve("dat-was-datps");
            if (Files.isDirectory(datps) && !datps.equals(sourceRoot)) {
                indexDirectory(datps, result);
            }
        }
        Path altDatps = Path.of("C:/eGovFrameDev-4.3.1-64bit/workspace/dat-was-datps");
        if (Files.isDirectory(altDatps) && !altDatps.equals(sourceRoot)) {
            indexDirectory(altDatps, result);
        }
        return Map.copyOf(result);
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
            CompilationUnit unit = javaParser.parse(path).getResult().orElse(null);
            if (unit == null) {
                diagnostics.add(sourceKind + " 분석 실패: " + relative(sourceRoot, path));
                return;
            }
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
