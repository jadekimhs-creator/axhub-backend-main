package io.shinhanlife.dat.report.application;

import io.shinhanlife.dat.report.excel.ToolReportExcelWriter;
import io.shinhanlife.dat.report.model.ToolReportModel;
import io.shinhanlife.dat.report.model.ToolSummary;
import io.shinhanlife.dat.report.model.ToolSourceType;
import io.shinhanlife.dat.report.source.ToolDetailAnalyzer;
import io.shinhanlife.dat.report.source.ToolSourceDiscovery;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * @package io.shinhanlife.dat.report.application
 * @className ToolReportService
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
@Service
public class ToolReportService {

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

        List<ToolReportModel> reports = selectedNames.stream().distinct()
                .map(sourceTools::get)
                .map(tool -> analyzer.analyze(tool, sourceRoot))
                .toList();
        return excelWriter.write(reports);
    }
}
