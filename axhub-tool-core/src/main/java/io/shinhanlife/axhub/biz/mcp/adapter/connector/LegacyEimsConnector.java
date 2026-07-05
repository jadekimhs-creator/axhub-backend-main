package io.shinhanlife.axhub.biz.mcp.adapter.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.axhub.biz.mcp.adapter.sender.EimsSender;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import io.shinhanlife.axhub.biz.mcp.adapter.util.LegacyDataTransformer;

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

        // 2. 4단계 라우팅 분기 처리
        String legacyResponse = null;
        if ("HTTP".equalsIgnoreCase(routingType)) {
            log.info("[라우팅] EIMS API (HTTP) 통신으로 전달");
            legacyResponse = httpEimsSender.send(interfaceId, payload);
        }
        else if ("TCP".equalsIgnoreCase(routingType)) {
            log.info("[라우팅] EIMS 소켓 (TCP) 통신으로 전달");
            legacyResponse = tcpEimsSender.send(interfaceId, payload);
        }
        else if ("JSP_FORM".equalsIgnoreCase(routingType)) {
            log.info("[라우팅] JSP Form 통신으로 전달");
            legacyResponse = jspFormEimsSender.send(interfaceId, payload);
        }
        else if ("JSP_JSON".equalsIgnoreCase(routingType)) {
            log.info("[라우팅] JSP JSON 통신으로 전달");
            legacyResponse = jspJsonEimsSender.send(interfaceId, payload);
        }
        else {
            //  3. 아키텍처 규격에 맞춘 라우팅 (실시간 vs 비동기)
            if (isMciRouting(routingType, interfaceId)) {
                log.info("[라우팅] 실시간 AI 요청 -> MCI 연계 어댑터를 통해 EIMS 전달");
                legacyResponse = mciEimsSender.send(interfaceId, payload);
            } else {
                log.info("[라우팅] 비동기/대용량 요청 -> EAI 연계 어댑터를 통해 EIMS 전달");
                legacyResponse = eaiEimsSender.send(interfaceId, payload);
            }
        }
        
        log.info("\n [Legacy -> Adapter] EIMS 통신 응답 수신: {}", legacyResponse);
        return legacyResponse;
    }

    //  라우팅 조건을 판단하는 내부 메서드 (관리를 위해 분리)
    private boolean isMciRouting(String routingType, String interfaceId) {
        // AI 에이전트가 툴을 호출하는 경우는 대부분 즉각적인 대답이 필요한 '실시간 조회(MCI)'입니다.
        return "MCI".equalsIgnoreCase(routingType) || (interfaceId != null && interfaceId.startsWith("MCI"));
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
                "{\"status\":\"CIRCUIT_OPEN\", \"message\":\"신한은행 내부 시스템 장애로 인해 일시적으로 차단되었습니다. 복구 후 재시도 부탁드립니다.\", \"interfaceId\":\"%s\"}",
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
}
