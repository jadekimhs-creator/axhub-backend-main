package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.VacationRegisterReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.LeaveCountReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.LeaveCountReq;

@McpTool(
    routingType = "HTTP"
)
public class CommonUtilityService extends AbstractMcpToolService {

    @McpFunction(name = "register_vacation", description = "휴가 등록", prompt = "내일 하루 연차 휴가를 등록해줘.", mappingId = "HR_VAC_01")
    public Object registerVacation(VacationRegisterReq data) {
        return executeLegacy("HTTP", "HR_VAC_01", data);
    }

    @McpFunction(name = "get_leave_count", description = "연차 갯수 조회", prompt = "현재 사용 가능한 남은 연차 일수를 알려줘.", mappingId = "HR_VAC_02")
    public Object getLeaveCount(LeaveCountReq data) {
        return executeLegacy("HTTP", "HR_VAC_02", data);
    }

}
