package io.shinhanlife.axhub.biz.mcp.tool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.dto
 * @className HrTestRes
 * @description AX HUB 시스템 처리 클래스
 * @author system
 * @create 2026.07.13
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.07.13  system    최초생성
 *
 * </pre>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HrTestRes {
    private String status;
    private String message;
    // TODO: Add response fields here
}
