package io.shinhanlife.axhub.common.mcp.tool.dto;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveCountReq {

    @McpParameter(description = "연차 내역을 조회할 사원 번호", required = true)
    private String employeeId;
}
