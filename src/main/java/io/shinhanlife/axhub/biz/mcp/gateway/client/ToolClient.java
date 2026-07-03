package io.shinhanlife.axhub.biz.mcp.gateway.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.axhub.biz.mcp.adapter.connector.LegacyEimsConnector;
import io.shinhanlife.axhub.biz.mcp.adapter.connector.ThirdPartySecurityConnector;
import io.shinhanlife.axhub.biz.mcp.adapter.dto.JsonRpcRequest;
import io.shinhanlife.axhub.biz.mcp.adapter.dto.JsonRpcResponse;
import io.shinhanlife.axhub.biz.mcp.adapter.dto.Params;
import io.shinhanlife.axhub.biz.mcp.adapter.util.PiiMaskingUtils;
import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.axhub.common.mcp.security.SecurityProperties;
import io.shinhanlife.axhub.biz.mcp.gateway.messaging.KafkaProducerService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.core.ParameterizedTypeReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolClient {

    private final SecurityProperties securityProperties;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final RetryRegistry retryRegistry;
    private final KafkaProducerService kafkaProducerService;
    private final RestClient restClient = RestClient.builder().build();

    private final LegacyEimsConnector legacyEimsConnector;
    private final ThirdPartySecurityConnector thirdPartySecurityConnector;
    private final ObjectMapper objectMapper;

    /**
     * 실제 Tool Pod를 호출합니다. (동적 서킷 브레이커 및 속도 제어 적용)
     */
    public Object call(Object targetPod, Map<String, Object> payload, Object planObj) {
        log.info("[ToolClient] RPC 통신 준비 - 동적 제어 정책 적용");
        
        ToolMetadata plan = (ToolMetadata) planObj;
        String toolName = plan.getToolName();

        int failureRate = plan.getFailureRateThreshold() != null ? plan.getFailureRateThreshold() : 50;
        int slidingWindowSize = plan.getSlidingWindowSize() != null ? plan.getSlidingWindowSize() : 10;
        
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRate)
                .slidingWindowSize(slidingWindowSize)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(toolName, cbConfig);

        int limitForPeriod = plan.getRateLimitForPeriod() != null ? plan.getRateLimitForPeriod() : 50;
        
        RateLimiterConfig rlConfig = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(0))
                .build();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(toolName, rlConfig);

        RetryConfig rtConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .build();
        Retry retry = retryRegistry.retry(toolName, rtConfig);

        String internalApiKey = securityProperties.getApiKeys().keySet().iterator().next();

        try {
            Supplier<Map<String, Object>> supplier = () -> {
                if (plan.getEndpoint() != null) {
                    Map<String, Object> targetMap = (Map<String, Object>) targetPod;
                    String baseUrl = (String) targetMap.get("targetUrl");
                    String targetUrl = baseUrl + plan.getEndpoint();
                    log.info("[ToolClient] 동적 엔드포인트 호출: {}", targetUrl);
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("X-API-KEY", internalApiKey);
                    
                    return restClient.post()
                            .uri(targetUrl)
                            .headers(h -> h.addAll(headers))
                            .body(payload)
                            .retrieve()
                            .body(new ParameterizedTypeReference<Map<String, Object>>() {});
                } else {
                    // 레거시 하드코딩 라우팅 (Adapter 로컬 직접 호출)
                    log.info("[ToolClient] 레거시 라우팅: Adapter 로컬 모듈 호출");
                    try {
                        JsonRpcRequest request = objectMapper.convertValue(payload, JsonRpcRequest.class);
                        Params params = request.getParams();

                        if (params == null) {
                            throw new IllegalArgumentException("Invalid params");
                        }

                        String routingType = params.getRoutingType();
                        String interfaceId = params.getInterfaceId();

                        String executionResult;
                        if (interfaceId != null && (interfaceId.startsWith("DRM_") || interfaceId.startsWith("BM_"))) {
                            executionResult = thirdPartySecurityConnector.executeSecurityModule(interfaceId, params.getData());
                        } else {
                            executionResult = legacyEimsConnector.executeByTool(routingType, interfaceId, params.getData(), params.getSpec());
                        }

                        String maskedResult = PiiMaskingUtils.mask(executionResult);

                        JsonRpcResponse response = new JsonRpcResponse();
                        response.setId(request.getId() != null ? request.getId() : UUID.randomUUID().toString());
                        response.setResult(maskedResult);
                        
                        return objectMapper.convertValue(response, Map.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Adapter 로컬 호출 실패: " + e.getMessage(), e);
                    }
                }
            };
            
            Supplier<Map<String, Object>> rateLimitedSupplier = RateLimiter.decorateSupplier(rateLimiter, supplier);
            Supplier<Map<String, Object>> retryingSupplier = Retry.decorateSupplier(retry, rateLimitedSupplier);
            Supplier<Map<String, Object>> protectedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, retryingSupplier);
            
            Map<String, Object> response = protectedSupplier.get();

            log.info(" [ToolClient] RPC 호출 성공 (적용된 제어기: {})", toolName);
            return response;

        } catch (RequestNotPermitted e) {
            log.warn(" [ToolClient] 트래픽 폭주 감지! Kafka 대기열로 요청을 전환합니다. (Tool: {})", toolName);
            
            String ticketId = kafkaProducerService.queueRequest("mcp.tool.execute.queue", payload);
            
            return Map.of(
                "status", "QUEUED",
                "ticketId", ticketId,
                "message", "현재 트래픽 폭주로 인해 대기열에 등록되었습니다. 티켓 ID를 통해 결과를 확인해주세요."
            );
            
        } catch (Exception e) {
            log.error(" [ToolClient] RPC 통신 실패 또는 차단됨: {}", e.getMessage());
            throw new RuntimeException("Tool Pod 호출 중 예외/차단 발생: " + e.getMessage(), e);
        }
    }
}
