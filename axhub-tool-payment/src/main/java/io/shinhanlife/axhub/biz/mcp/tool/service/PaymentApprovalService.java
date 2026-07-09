package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.PaymentApprovalReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.PaymentApprovalRes;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.UUID;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.service
 * @className PaymentApprovalService
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
@Service
@McpTool(
    routingType = "MCI",
    group = "PAYMENT"
)
public class PaymentApprovalService extends AbstractMcpToolService {

    @McpFunction(
        name = "paymentapproval",
        description = "결제 승인 처리",
        prompt = "결제 승인 처리 해줘.",
        mappingId = "PAY_001"
    )
    public Object execute(PaymentApprovalReq req) {
        log.info("[Payment] 결제 승인 요청 수신. 계좌: {}, 금액: {}", req.getAccountNumber(), req.getAmount());
        
        // 레거시 연동
        Map<String, Object> result = executeLegacy("MCI", "PAY_001", req);
        
        // 가짜 데이터 응답 매핑 (AI 에이전트가 그럴듯하게 보여주기 위함)
        if ("SUCCESS".equals(result.get("status"))) {
            result.put("transactionId", "TX_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            result.put("approvedAmount", req.getAmount());
            result.put("message", "결제가 성공적으로 승인되었습니다.");
        }
        
        return result;
    }
}
