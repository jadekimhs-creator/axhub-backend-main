package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.BalanceReq;
import io.shinhanlife.axhub.biz.mcp.tool.service.AbstractMcpToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@McpTool(routingType = "MCI", group = "COMMON")
public class BalanceService extends AbstractMcpToolService {

    @McpFunction(
        name = "balance",
        description = "고객의 계좌 잔액을 조회합니다.",
        prompt = "고객 계좌 잔액을 조회해줘.",
        mappingId = "ACC_001"
    )
    public Object execute(BalanceReq req) {
        log.info("[Balance] 계좌 잔액 조회 요청 수신. 계좌번호: {}", req.getAccountNumber());
        
        // 레거시 연동
        Map<String, Object> result = executeLegacy("MCI", "ACC_001", req);
        
        // 가짜 데이터 응답 매핑 (AI 에이전트가 그럴듯하게 보여주기 위함)
        if ("SUCCESS".equals(result.get("status"))) {
            result.put("accountNumber", req.getAccountNumber());
            result.put("balance", 1520300); // 1,520,300원 (가상의 잔액)
            result.put("currency", "KRW");
            result.put("message", "잔액 조회가 완료되었습니다.");
        }
        
        return result;
    }
}
