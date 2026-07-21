package io.shinhanlife.dap.mcc.other.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import io.shinhanlife.dap.mcc.dto.BillingProcessReq;
import io.shinhanlife.dap.mcc.dto.BillingStatusReq;
import io.shinhanlife.dap.mcc.service.AbstractMcpToolService;

import java.util.HashMap;
import java.util.Map;

@McpTool(
    routingType = "MCI",
    categoryKey = "claim"
)
/**
 * @package io.shinhanlife.dap.mcc.service
 * @className BillingProcessService
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
@lombok.extern.slf4j.Slf4j
public class BillingProcessService extends AbstractMcpToolService {

    @McpFunction(register = false, displayName = "status 툴", name = "status", description = "청구심사 상태 조회", prompt = "현재 접수된 청구건 상태를 알려줘.", mappingId = "BILL_001")
    public Object getStatus(BillingStatusReq data) {
        return executeBillingLogic("BILL_001", data);
    }

    @McpFunction(register = false, displayName = "process 툴", name = "process", description = "청구 처리", prompt = "현재 접수된 청구건에 대한 심사 처리를 진행해.", mappingId = "BILL_002")
    public Object processBilling(BillingProcessReq data) {
        return executeBillingLogic("BILL_002", data);
    }

    private Object executeBillingLogic(String mappingId, Object data) {
        log.info(" [Billing] 청구 처리 전용 커스텀 전/후처리 로직 수행 시작");
        
        Map<String, Object> payload;
        if (data == null) {
            payload = new HashMap<>();
        } else {
            ObjectMapper mapper = new ObjectMapper();
            payload = mapper.convertValue(data, new TypeReference<Map<String, Object>>() {});
        }
        
        // 커스텀 전처리
        payload.put("custom_injected_data", "Billing System Check OK");
        log.info(" [Billing] 커스텀 파라미터 주입 완료");

        // 부모 클래스의 레거시 공통 연동 메서드 호출 (PII 마스킹 포함)
        Map<String, Object> result = executeLegacy("MCI", mappingId, payload);

        // 커스텀 후처리
        if ("SUCCESS".equals(result.get("status"))) {
            result.put("billing_custom_insight", "청구 특화 후처리 로직이 적용되었습니다.");
        }
        return result;
    }
}