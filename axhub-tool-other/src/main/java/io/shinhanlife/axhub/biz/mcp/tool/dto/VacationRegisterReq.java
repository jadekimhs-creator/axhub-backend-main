package io.shinhanlife.axhub.biz.mcp.tool.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.dto
 * @className VacationRegisterReq
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
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationRegisterReq {

    @McpParameter(description = "연차를 등록할 사원 번호", required = true)
    private String employeeId;

    @McpParameter(description = "휴가 일자 (YYYY-MM-DD 형식)", required = true)
    private String date;
}