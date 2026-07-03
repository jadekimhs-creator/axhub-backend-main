package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.VacationRegisterReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.LeaveCountReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.SmsSendReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.EmailSendReq;

@McpTool(
    name = "common_utility_tool", 
    description = "전사 공통 유틸리티 툴 (HR 및 알림)", 
    group = "biz_core", 
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

    @McpFunction(name = "send_sms", description = "SMS 발송", prompt = "고객님께 심사 완료 안내 SMS 문자를 발송해줘.", mappingId = "COM_SMS_01")
    public Object sendSms(SmsSendReq data) {
        return executeLegacy("HTTP", "COM_SMS_01", data);
    }

    @McpFunction(name = "send_email", description = "이메일 발송", prompt = "담당자에게 결과 보고서 이메일을 보내줘.", mappingId = "COM_EML_01")
    public Object sendEmail(EmailSendReq data) {
        return executeLegacy("HTTP", "COM_EML_01", data);
    }
}
