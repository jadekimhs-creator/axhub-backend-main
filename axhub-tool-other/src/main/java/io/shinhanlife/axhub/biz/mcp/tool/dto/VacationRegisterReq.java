package io.shinhanlife.axhub.biz.mcp.tool.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

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
