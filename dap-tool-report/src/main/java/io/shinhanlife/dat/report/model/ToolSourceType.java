package io.shinhanlife.dat.report.model;

import java.util.Locale;

/**
 * @package io.shinhanlife.dat.report.model
 * @className ToolSourceType
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
public enum ToolSourceType {
    DAP_WAS_DAPMT,
    DAP_ADMIN,
    DAT_WAS_DATPS;

    public static ToolSourceType from(String value) {
        if (value == null || value.isBlank()) return DAP_WAS_DAPMT;
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        if ("DAT_WAS_DATMT".equals(normalized)) return DAP_WAS_DAPMT;
        if ("AXHUB_BACKEND".equals(normalized) || "AXHUB_BACKEND_MAIN".equals(normalized)) return DAP_ADMIN;
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 Tool 소스 유형입니다: " + value);
        }
    }
}
