package io.shinhanlife.dat.report.application;

import io.shinhanlife.dat.report.excel.ToolReportExcelWriter;
import io.shinhanlife.dat.report.model.ToolReportModel;
import io.shinhanlife.dat.report.model.ToolSourceType;
import io.shinhanlife.dat.report.model.ToolSummary;
import io.shinhanlife.dat.report.source.ToolDetailAnalyzer;
import io.shinhanlife.dat.report.source.ToolSourceDiscovery;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 툴 소스를 탐색하고 툴 파일 기준으로 Excel 보고서를 생성하는 서비스.
 */
@Service
public class ToolReportService {

    public record ReportExportResult(byte[] content, String filename, String contentType) {
    }

    private final ToolSourceDiscovery discovery;
    private final ToolDetailAnalyzer analyzer;
    private final ToolReportExcelWriter excelWriter;

    public ToolReportService(ToolSourceDiscovery discovery,
                             ToolDetailAnalyzer analyzer,
                             ToolReportExcelWriter excelWriter) {
        this.discovery = discovery;
        this.analyzer = analyzer;
        this.excelWriter = excelWriter;
    }

    public List<ToolSummary> findTools(String sourceType) {
        return findTools(sourceType, null);
    }

    public List<ToolSummary> findTools(String sourceType, String projectPath) {
        if (projectPath != null && !projectPath.isBlank()) {
            return discovery.discover(Path.of(projectPath.trim()).toAbsolutePath().normalize());
        }
        return discovery.discover(ToolSourceType.from(sourceType));
    }

    public byte[] createExcel(List<String> selectedNames, String sourceTypeValue) {
        return createExcel(selectedNames, sourceTypeValue, null);
    }

    public byte[] createExcel(List<String> selectedNames, String sourceTypeValue, String projectPath) {
        return exportReports(selectedNames, sourceTypeValue, projectPath).content();
    }

    public ReportExportResult exportReports(List<String> selectedNames, String sourceTypeValue, String projectPath) {
        if (selectedNames == null || selectedNames.isEmpty()) {
            throw new IllegalArgumentException("보고서에 포함할 툴을 하나 이상 선택해야 합니다.");
        }

        Path sourceRoot = (projectPath != null && !projectPath.isBlank())
                ? Path.of(projectPath.trim()).toAbsolutePath().normalize()
                : discovery.sourceRoot(ToolSourceType.from(sourceTypeValue));

        Map<String, ToolSummary> sourceTools = new LinkedHashMap<>();
        discovery.discover(sourceRoot).forEach(tool -> sourceTools.put(tool.name(), tool));
        List<String> missingSources = selectedNames.stream()
                .filter(name -> !sourceTools.containsKey(name)).distinct().toList();
        if (!missingSources.isEmpty()) {
            throw new IllegalArgumentException("분석할 원천 소스가 없는 툴입니다: " + missingSources);
        }

        Map<String, Path> javaFiles = analyzer.indexJavaFiles(sourceRoot);
        List<ToolReportModel> reports = selectedNames.stream().distinct()
                .map(sourceTools::get)
                .map(tool -> analyzer.analyze(tool, sourceRoot, javaFiles))
                .toList();

        if (reports.size() == 1) {
            ToolReportModel single = reports.get(0);
            byte[] content = excelWriter.writeSingleTool(single, sourceRoot);
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String filename = buildReportFilename(single.tool(), date);
            return new ReportExportResult(content, filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            byte[] content = excelWriter.writeZip(reports, sourceRoot);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "tool-reports_" + timestamp + ".zip";
            return new ReportExportResult(content, filename, "application/zip");
        }
    }

    public static String buildReportFilename(ToolSummary tool, String date) {
        String toolName = sanitizeFilename(tool.name());
        String title = tool.title();
        String titlePart;
        if (title != null && !title.isBlank() && !title.equals(tool.name())) {
            titlePart = "(" + sanitizeFilename(title.trim()) + ")";
        } else {
            titlePart = "(설계서)";
        }
        return toolName + "_" + titlePart + "_v" + date + ".xlsx";
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "tool";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
