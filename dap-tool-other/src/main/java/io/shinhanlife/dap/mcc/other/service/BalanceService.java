package io.shinhanlife.dap.mcc.other.service;


/**
 * @package io.shinhanlife.dap.mcc.service
 * @className BalanceService
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
import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import io.shinhanlife.dap.mcc.dto.BalanceReq;
import io.shinhanlife.dap.mcc.service.AbstractMcpToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@McpTool(routingType = "MCI", categoryKey = "common")
public class BalanceService extends AbstractMcpToolService {

    @McpFunction(register = false, displayName = "balance 툴", name = "balance",
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
