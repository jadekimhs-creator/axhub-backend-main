package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.adapter.connector.LegacyEimsConnector;
import io.shinhanlife.axhub.biz.mcp.adapter.util.PiiMaskingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractMcpToolService {

    @Autowired
    protected LegacyEimsConnector legacyEimsConnector;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * 레거시 시스템을 호출하고 공통 처리(PII 마스킹 등)를 수행합니다.
     */
    protected Map<String, Object> executeLegacy(String routingType, String interfaceId, Object inputData) {
        Map<String, Object> inputMap;
        if (inputData == null) {
            inputMap = new HashMap<>();
        } else if (inputData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) inputData;
            inputMap = map;
        } else {
            inputMap = objectMapper.convertValue(inputData, new TypeReference<Map<String, Object>>() {});
        }

        try {
            log.info("\n [Tool -> Adapter] 레거시 실행 요청 - RoutingType: {}, Interface: {}, Data: {}", routingType, interfaceId, inputMap);
            // 1. Adapter 공통 모듈 직접 호출
            String executionResult = legacyEimsConnector.executeByTool(routingType, interfaceId, inputMap, null);
            log.info("\n [Adapter -> Tool] 레거시 실행 응답 수신: {}", executionResult);
            
            // 2. PII 마스킹 처리
            String maskedResult = PiiMaskingUtils.mask(executionResult);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("legacy_response", maskedResult);
            return result;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return error;
        }
    }
}
