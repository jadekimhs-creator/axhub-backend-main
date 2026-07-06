package io.shinhanlife.axhub.biz.mcp.tool.sms;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.SmsSendReq;
import io.shinhanlife.axhub.biz.mcp.tool.service.AbstractMcpToolService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@McpTool(routingType = "EAI", group = "NOTIFICATION")
public class SmsToolService extends AbstractMcpToolService {

    @McpFunction(name = "send_sms", description = "SMS 발송", prompt = "고객에게 SMS 메시지를 발송해줘.", mappingId = "SMS_SEND_001")
    public Object sendSms(SmsSendReq req) {
        log.info("📱 [SMS] SMS 발송 요청 수신. 수신자: {}", req.getPhoneNumber());

        // EAI 연동을 위한 파라미터 변환
        Map<String, Object> payload = new HashMap<>();
        payload.put("phone", req.getPhoneNumber());
        payload.put("content", req.getMessage());

        // 레거시 시스템 연동 (EAI)
        Map<String, Object> result = executeLegacy("EAI", "SMS_SEND_001", payload);

        // 결과 가공
        if ("SUCCESS".equals(result.get("status"))) {
            result.put("message", "SMS가 성공적으로 발송되었습니다.");
        }

        return result;
    }
}
