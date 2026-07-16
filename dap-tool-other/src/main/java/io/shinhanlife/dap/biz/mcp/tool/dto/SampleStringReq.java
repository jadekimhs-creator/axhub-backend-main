package io.shinhanlife.dap.biz.mcp.tool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * @package io.shinhanlife.dap.biz.mcp.tool.dto
 * @className SampleStringReq
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SampleStringReq {
    private String query;
}
