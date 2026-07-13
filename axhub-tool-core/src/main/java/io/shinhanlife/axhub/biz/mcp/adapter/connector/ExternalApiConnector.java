package io.shinhanlife.axhub.biz.mcp.adapter.connector;

import io.shinhanlife.axhub.biz.mcp.adapter.support.DynamicPayloadBuilder;
import io.shinhanlife.axhub.biz.mcp.adapter.support.DynamicSchemaValidator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;
import java.util.List;

/**
 * @package io.shinhanlife.axhub.biz.mcp.adapter.connector
 * @className ExternalApiConnector
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
public class ExternalApiConnector {

    private final RestClient restClient;
    private final DynamicSchemaValidator schemaValidator;
    private final DynamicPayloadBuilder payloadBuilder;

    public ExternalApiConnector(DynamicSchemaValidator schemaValidator, DynamicPayloadBuilder payloadBuilder) {
        this.restClient = RestClient.create();
        this.schemaValidator = schemaValidator;
        this.payloadBuilder = payloadBuilder;
    }

    @RateLimiter(name = "externalApi", fallbackMethod = "fallbackForExternalApi")
    @CircuitBreaker(name = "externalApi", fallbackMethod = "fallbackForExternalApi")
    public String callExternalApi(String apiName, String endpoint, Map<String, Object> data, List<Map<String, Object>> spec, boolean isFixedLength) throws Exception {

        // 1. 요청 데이터 검증 (errorLog 전달하여 구체적 에러 포착)
        StringBuilder errorLog = new StringBuilder();
        if (!schemaValidator.validate(spec, data, errorLog)) {
            String errorMsg = "API 요청 데이터 스키마 불일치 [" + apiName + "]: " + errorLog.toString();
            log.error(" {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // 2. 페이로드 빌드
        Object payload = isFixedLength ? payloadBuilder.buildFixedLengthString(spec, data) : data;
        String contentType = isFixedLength ? "application/x-www-form-urlencoded;charset=EUC-KR" : "application/json";

        log.info(" 외부 API 호출 [{}] 시작 (FixedLength: {})", apiName, isFixedLength);

        return restClient.post()
                .uri(endpoint)
                .contentType(MediaType.parseMediaType(contentType))
                .body(payload)
                .retrieve()
                .body(String.class);
    }

    public String fallbackForExternalApi(String apiName, String endpoint, Map<String, Object> data, List<Map<String, Object>> spec, boolean isFixedLength, Throwable t) {
        log.error(" [외부 API 장애] {} 호출 실패: {}", apiName, t.getMessage());
        return String.format("{\"status\":\"EXTERNAL_API_ERROR\", \"message\":\"외부 서비스 연동 중 오류 발생: %s\"}", t.getMessage());
    }
}