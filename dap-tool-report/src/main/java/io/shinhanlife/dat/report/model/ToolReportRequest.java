package io.shinhanlife.dat.report.model;

import java.util.List;

/** 사용자가 선택한 툴 보고서 생성 요청. */
public record ToolReportRequest(List<String> toolNames, String sourceType) {
}
