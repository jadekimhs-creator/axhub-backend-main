package io.shinhanlife.axhub.biz.mcp.tool.sms;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.SmsSendReq;
import io.shinhanlife.axhub.biz.mcp.tool.service.AbstractMcpToolService;
import io.shinhanlife.axhub.biz.mcp.tool.sms.dto.SmsMciReqDto;
import io.shinhanlife.axhub.biz.mcp.tool.sms.mapper.SmsMciMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.sms
 * @className SmsToolService
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
@Slf4j
@McpTool(routingType = "EAI", group = "NOTIFICATION")
public class SmsToolService extends AbstractMcpToolService {

    @McpFunction(name = "send_sms", description = "SMS 발송", prompt = "고객에게 SMS 메시지를 발송해줘.", mappingId = "SMS_SEND_001")
    public Object sendSms(SmsSendReq req) {
        log.info("📱 [SMS] SMS 발송 요청 수신. 수신자: {}", req.getPhoneNumber());

        // MapStruct를 이용한 자동 매핑 (AI DTO -> MCI DTO)
        SmsMciReqDto mciReq = SmsMciMapper.INSTANCE.toMciReq(req);

        // 레거시 시스템 연동 (EAI) - DTO 객체를 그대로 넘김
        Map<String, Object> result = executeLegacy("EAI", "SMS_SEND_001", mciReq);

        // 결과 가공
        if ("SUCCESS".equals(result.get("status"))) {
            result.put("message", "SMS가 성공적으로 발송되었습니다.");
        }

        return result;
    }
}