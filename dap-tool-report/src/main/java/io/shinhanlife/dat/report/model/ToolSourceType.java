package io.shinhanlife.dat.report.model;

import java.util.Locale;

/** Report 생성 시 참조할 Tool 소스 프로젝트. */
public enum ToolSourceType {
    DAP_WAS_DAPMT,
    DAP_ADMIN;

    public static ToolSourceType from(String value) {
        if (value == null || value.isBlank()) return DAP_WAS_DAPMT;
        try {
            return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 Tool 소스 유형입니다: " + value);
        }
    }
}
