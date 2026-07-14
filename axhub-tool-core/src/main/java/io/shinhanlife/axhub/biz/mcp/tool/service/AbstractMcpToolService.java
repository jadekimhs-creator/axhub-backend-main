package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.adapter.connector.LegacyEimsConnector;
import io.shinhanlife.axhub.biz.mcp.adapter.util.PiiMaskingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.service
 * @className AbstractMcpToolService
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
public abstract class AbstractMcpToolService {

    @Autowired
    protected LegacyEimsConnector legacyEimsConnector;

    @Autowired
    protected ObjectMapper objectMapper;

    protected Map<String, Object> executeLegacy(String routingType, String interfaceId, Object inputData) {
        return executeLegacy(routingType, interfaceId, inputData, null);
    }

    /**
     * 레거시 시스템을 호출하고 공통 처리(PII 마스킹 등)를 수행합니다. (스펙 지정 가능)
     */
    protected Map<String, Object> executeLegacy(String routingType, String interfaceId, Object inputData, List<Map<String, Object>> spec) {
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
            log.info("\n=======================================================");
            log.info(" [Tool -> Legacy] 레거시 시스템 통신 시작");
            log.info(" - Routing Type: {}", routingType);
            log.info(" - Interface ID: {}", interfaceId);
            log.info(" - 호출 파라미터 (Request): \n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputMap));
            log.info("=======================================================\n");

            // 1. Adapter 공통 모듈 직접 호출
            String executionResult = legacyEimsConnector.executeByTool(routingType, interfaceId, inputMap, spec);

            log.info("\n=======================================================");
            log.info(" [Legacy -> Tool] 레거시 시스템 통신 완료");
            log.info(" - 응답 파라미터 (Response, 마스킹 전): \n{}", executionResult);
            log.info("=======================================================\n");
            
            // 2. PII 마스킹 처리
            String maskedResult = PiiMaskingUtils.mask(executionResult);

            if (executionResult != null && (executionResult.contains("\"status\":\"CIRCUIT_OPEN\"") || executionResult.contains("\"status\":\"TOO_MANY_REQUESTS\""))) {
                try {
                    return objectMapper.readValue(executionResult, new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    log.error("Fallback JSON 파싱 에러: {}", e.getMessage());
                }
            }

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