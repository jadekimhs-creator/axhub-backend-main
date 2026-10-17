package io.shinhanlife.dat.report.model;

import java.util.List;

/**
 * @package io.shinhanlife.dat.report.model
 * @className ToolReportRequest
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
public record ToolReportRequest(List<String> toolNames, String sourceType, String projectPath) {
    public ToolReportRequest(List<String> toolNames, String sourceType) {
        this(toolNames, sourceType, null);
    }
}
