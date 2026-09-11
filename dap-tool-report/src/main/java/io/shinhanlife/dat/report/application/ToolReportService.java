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

/** Gateway가 선택해 전달한 툴의 원천 분석과 Excel 출력을 담당한다. */
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
        return discovery.discover(ToolSourceType.from(sourceType));
    }

    public byte[] createExcel(List<String> selectedNames, String sourceTypeValue) {
        if (selectedNames == null || selectedNames.isEmpty()) {
            throw new IllegalArgumentException("보고서에 포함할 툴을 하나 이상 선택해야 합니다.");
        }

        ToolSourceType sourceType = ToolSourceType.from(sourceTypeValue);
        Map<String, ToolSummary> sourceTools = new LinkedHashMap<>();
        discovery.discover(sourceType).forEach(tool -> sourceTools.put(tool.name(), tool));
        List<String> missingSources = selectedNames.stream()
                .filter(name -> !sourceTools.containsKey(name)).distinct().toList();
        if (!missingSources.isEmpty()) {
            throw new IllegalArgumentException("분석할 원천 소스가 없는 툴입니다: " + missingSources);
        }

        List<ToolReportModel> reports = selectedNames.stream().distinct()
                .map(sourceTools::get)
                .map(tool -> analyzer.analyze(tool, sourceType))
                .toList();
        return excelWriter.write(reports);
    }
}
