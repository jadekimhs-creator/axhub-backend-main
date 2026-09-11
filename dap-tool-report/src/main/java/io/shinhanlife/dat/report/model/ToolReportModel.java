package io.shinhanlife.dat.report.model;

import java.util.List;

/** 수집 단계와 Excel 출력 단계를 분리하는 표준 중간 모델. */
public record ToolReportModel(ToolSummary tool, List<FieldDefinition> fields, List<String> diagnostics) {
}
