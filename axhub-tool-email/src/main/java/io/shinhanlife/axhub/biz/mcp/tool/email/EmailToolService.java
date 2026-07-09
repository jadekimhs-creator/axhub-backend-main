package io.shinhanlife.axhub.biz.mcp.tool.email;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.EmailSendReq;
import io.shinhanlife.axhub.biz.mcp.tool.service.AbstractMcpToolService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.email
 * @className EmailToolService
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
public class EmailToolService extends AbstractMcpToolService {

    @McpFunction(name = "send_email", description = "이메일 발송", prompt = "고객에게 이메일을 발송해줘.", mappingId = "EMAIL_SEND_001")
    public Object sendEmail(EmailSendReq req) {
        log.info("📧 [Email] 이메일 발송 요청 수신. 수신자: {}", req.getEmailAddress());

        // EAI 연동을 위한 파라미터 변환
        Map<String, Object> payload = new HashMap<>();
        payload.put("address", req.getEmailAddress());
        payload.put("subject", req.getSubject());
        payload.put("content", req.getBody());

        // 레거시 시스템 연동 (EAI)
        Map<String, Object> result = executeLegacy("EAI", "EMAIL_SEND_001", payload);

        // 결과 가공
        if ("SUCCESS".equals(result.get("status"))) {
            result.put("message", "이메일이 성공적으로 발송되었습니다.");
        }

        return result;
    }
}