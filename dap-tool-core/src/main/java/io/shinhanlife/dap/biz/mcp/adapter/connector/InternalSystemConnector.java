package io.shinhanlife.dap.biz.mcp.adapter.connector;

import io.shinhanlife.dap.biz.mcp.adapter.support.DynamicPayloadBuilder;
import io.shinhanlife.dap.biz.mcp.adapter.support.DynamicSchemaValidator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;
import java.util.List;

/**
 * @package io.shinhanlife.dap.biz.mcp.adapter.connector
 * @className InternalSystemConnector
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
public class InternalSystemConnector {

    private final RestClient restClient;
    private final DynamicSchemaValidator schemaValidator;
    private final DynamicPayloadBuilder payloadBuilder;

    // 생성자를 통한 의존성 주입 (Spring이 알아서 Validator와 Builder를 넣어줍니다)
    public InternalSystemConnector(DynamicSchemaValidator schemaValidator, DynamicPayloadBuilder payloadBuilder) {
        this.restClient = RestClient.create();
        this.schemaValidator = schemaValidator;
        this.payloadBuilder = payloadBuilder;
    }

    @RateLimiter(name = "internalSystem", fallbackMethod = "fallbackForInternal")
    @CircuitBreaker(name = "internalSystem", fallbackMethod = "fallbackForInternal")
    public String callInternalSystem(String targetName, String endpoint, Map<String, Object> data, List<Map<String, Object>> spec, boolean isFixedLength) throws Exception {

        // 1. 요청 데이터 검증 (errorLog 전달하여 구체적 에러 포착)
        StringBuilder errorLog = new StringBuilder();
        if (!schemaValidator.validate(spec, data, errorLog)) {
            String errorMsg = "대내외 시스템 연계 데이터 스키마 불일치 [" + targetName + "]: " + errorLog.toString();
            log.error(" {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // 2. 페이로드 빌드 (고정장 vs JSON)
        Object payload = isFixedLength ? payloadBuilder.buildFixedLengthString(spec, data) : data;
        String contentType = isFixedLength ? "application/x-www-form-urlencoded;charset=EUC-KR" : "application/json";

        log.info(" 대내외 시스템 호출 [{}] 시작 (FixedLength: {})", targetName, isFixedLength);

        return restClient.post()
                .uri(endpoint)
                .contentType(MediaType.parseMediaType(contentType))
                .body(payload)
                .retrieve()
                .body(String.class);
    }

    // 통신 장애(CircuitBreaker) 또는 허용량 초과(RateLimiter) 시 처리 로직
    public String fallbackForInternal(String targetName, String endpoint, Map<String, Object> data, List<Map<String, Object>> spec, boolean isFixedLength, Throwable t) {
        log.error(" [대내외 시스템 장애/지연] {} 연계 실패: {}", targetName, t.getMessage());
        return String.format("{\"status\":\"INTERNAL_SYSTEM_ERROR\", \"message\":\"대내외 연계 시스템 호출 중 오류가 발생했거나 요청이 지연되었습니다. 사유: %s\"}", t.getMessage());
    }
}