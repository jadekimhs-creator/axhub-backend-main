package io.shinhanlife.dap.mcc.service;

import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;

/**
 * @package io.shinhanlife.dap.mcc.service
 * @className ExchangeRateToolService
 * @description 실시간 환율 조회 툴
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
    routingType = "DIRECT",
    categoryKey = "sample"
)
public class ExchangeRateToolService extends AbstractMcpToolService {

    public record ExchangeRateReq(String currencyCode) {}
    public record ExchangeRateRes(String baseCurrency, String targetCurrency, double rate) {}

    private final RestClient restClient;

    public ExchangeRateToolService() {
        this.restClient = RestClient.create();
    }

    @McpFunction(register = true, displayName = "실시간 환율 조회 툴", name = "exchange_rate",
        description = "원하는 통화의 실시간 환율을 조회합니다. (예: USD, EUR, JPY)",
        prompt = "현재 달러 환율 알려줘, 엔화 환율은?",
        mappingId = "EXCHANGE_001"
    )
    public ExchangeRateRes execute(ExchangeRateReq req) {
        String targetCurrency = req.currencyCode() != null ? req.currencyCode().toUpperCase().trim() : "USD";
        
        // 간단한 모의 데이터로 반환 (실제 구현 시 외부 연동)
        double dummyRate = 1350.50;
        if (targetCurrency.contains("JPY")) {
            dummyRate = 905.20;
        } else if (targetCurrency.contains("EUR")) {
            dummyRate = 1450.30;
        }

        log.info("[ExchangeRateTool] 환율 조회 완료: {} -> {}", targetCurrency, dummyRate);
        return new ExchangeRateRes("KRW", targetCurrency, dummyRate);
    }
}
