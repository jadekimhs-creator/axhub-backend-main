package io.shinhanlife.dap.biz.mcp.adapter.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dap.biz.mcp.adapter.sender.EimsSender;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import io.shinhanlife.dap.biz.mcp.adapter.util.LegacyDataTransformer;

/**
 * @package io.shinhanlife.dap.biz.mcp.adapter.connector
 * @className LegacyEimsConnector
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
@RequiredArgsConstructor
public class LegacyEimsConnector {

    // 4가지 방식의 Sender를 모두 주입받습니다. (변수명이 아주 중요합니다!)
    private final EimsSender httpEimsSender;      // 1~20 (EIMS API)
    private final EimsSender tcpEimsSender;       // 21~30 (EIMS Socket)
    private final EimsSender jspFormEimsSender;   // 31~40 (JSP Form)
    private final EimsSender jspJsonEimsSender;   // 41~50 (JSP JSON)

    private final EimsSender mciEimsSender;   // 실시간 연계 (기존 HTTP/TCP 대체, 동기식 API)
    private final EimsSender mciStringEimsSender; // 실시간 연계 (String 전문 버전)
    private final EimsSender eaiEimsSender;   // 비동기/대용량 연계 (배치 통신 등)

    private final ObjectMapper jsonMapper;


    //  방어막 적용: 예외가 발생하거나 차단기가 열리면 fallbackMethod를 즉시 실행합니다!
    //  서킷 브레이커와 Rate Limiter를 동시에 적용 (둘 중 하나라도 걸리면 fallbackForEims 실행)
    @RateLimiter(name = "eims", fallbackMethod = "fallbackForEims")
    @CircuitBreaker(name = "eims", fallbackMethod = "fallbackForEims")
    //  2번 업그레이드 적용: 파라미터 기반의 스마트 캐싱 (고객의 요청 데이터가 다르면 캐시도 다르게 적용)
    @Cacheable(value = "eimsData", key = "#routingType + '-' + #interfaceId + '-' + (#data != null ? #data.hashCode() : 0)")
    public String executeByTool(String routingType, String interfaceId, Map<String, Object> data, List<Map<String, Object>> spec) throws Exception {

        // 1. 데이터 조립 (방어 로직 포함)
        String payload = buildPayload(data, spec);

        log.info("\n [Adapter -> Legacy] EIMS 통신 요청 - RoutingType: {}, Interface: {}, Payload: {}", routingType, interfaceId, payload);

        // 2. 4단계 라우팅 분기 처리 (Switch Expression 활용)
        String upperRoutingType = routingType != null ? routingType.toUpperCase() : "";
        
        String legacyResponse = switch (upperRoutingType) {
            case "HTTP" -> {
                log.info("[라우팅] EIMS API (HTTP) 통신으로 전달");
                yield httpEimsSender.send(interfaceId, payload);
            }
            case "TCP" -> {
                log.info("[라우팅] EIMS 소켓 (TCP) 통신으로 전달");
                yield tcpEimsSender.send(interfaceId, payload);
            }
            case "JSP_FORM" -> {
                log.info("[라우팅] JSP Form 통신으로 전달");
                yield jspFormEimsSender.send(interfaceId, payload);
            }
            case "JSP_JSON" -> {
                log.info("[라우팅] JSP JSON 통신으로 전달");
                yield jspJsonEimsSender.send(interfaceId, payload);
            }

            case "MCI" -> {
                if (isStringMci(spec)) {
                    log.info("[라우팅] 스펙 자동 판별 (String 포맷 감지) -> MCI 연계 어댑터(String)를 통해 EIMS 전달");
                    yield mciStringEimsSender.send(interfaceId, payload);
                } else {
                    log.info("[라우팅] 실시간 AI 요청 -> MCI 연계 어댑터를 통해 EIMS 전달");
                    yield mciEimsSender.send(interfaceId, payload);
                }
            }
            case "EAI" -> {
                log.info("[라우팅] 비동기/대용량 요청 -> EAI 연계 어댑터를 통해 EIMS 전달");
                yield eaiEimsSender.send(interfaceId, payload);
            }
            default -> {
                log.error("[라우팅] 알 수 없는 라우팅 타입: {}", routingType);
                throw new IllegalArgumentException("지원하지 않는 라우팅 타입입니다: " + routingType);
            }
        };
        
        log.info("\n [Legacy -> Adapter] EIMS 통신 응답 수신: {}", legacyResponse);
        return legacyResponse;
    }


    //  비상용 응답 메서드 (차단기가 열려있거나, 타임아웃/에러가 났을 때 실행됨)
    // 주의: 파라미터는 원본 메서드와 100% 똑같이 맞추고, 마지막에 Throwable을 받아야 합니다.
    public String fallbackForEims(String routingType, String interfaceId, Map<String, Object> data, List<Map<String, Object>> spec, Throwable t) {

        // 1. Rate Limiter에 의해 차단된 경우 (트래픽 폭주)
        if (t instanceof RequestNotPermitted) {
            log.warn(" [Rate Limiter 발동] 트래픽 폭주로 요청 차단! interfaceId: {}", interfaceId);
            return String.format(
                    "{\"status\":\"TOO_MANY_REQUESTS\", \"message\":\"순간적인 요청 폭주로 인해 일시적으로 제한되었습니다. 잠시 후 시도해 주세요.\", \"interfaceId\":\"%s\"}",
                    interfaceId
            );
        }

        // 2. Circuit Breaker에 의해 차단된 경우 (레거시 시스템 장애/지연)
        log.error(" [서킷 브레이커 발동] 레거시 통신 차단! 원인: {}", t.getMessage());
        return String.format(
                "{\"status\":\"CIRCUIT_OPEN\", \"message\":\"신한라이프 내부 시스템 장애로 인해 일시적으로 차단되었습니다. 복구 후 재시도 부탁드립니다.\", \"interfaceId\":\"%s\"}",
                interfaceId
        );
    }

    private String buildPayload(Map<String, Object> data, List<Map<String, Object>> spec) {
        //  3번 항목 적용: 원본 데이터를 스펙에 맞게 엄격히 정제(변환, 형변환, 잘라내기, 기본값 등)
        Map<String, Object> transformedData = LegacyDataTransformer.transform(data, spec);

        if (transformedData == null || transformedData.isEmpty()) return "{}";
        
        try {
            return jsonMapper.writeValueAsString(transformedData);
        } catch (Exception e) {
            log.error(" JSON 변환 에러: {}", e.getMessage());
            return "{}";
        }
    }

    private boolean isStringMci(List<Map<String, Object>> spec) {
        if (spec == null || spec.isEmpty()) return false;
        // 스펙 내에 maxLength 등 고정길이 전문 관련 속성이 하나라도 존재하거나 명시적으로 STRING 힌트가 있으면 String 전문으로 간주
        return spec.stream().anyMatch(field -> 
            field.containsKey("maxLength") || 
            field.containsKey("byteSize") ||
            "STRING".equalsIgnoreCase(String.valueOf(field.get("mciFormat")))
        );
    }
}