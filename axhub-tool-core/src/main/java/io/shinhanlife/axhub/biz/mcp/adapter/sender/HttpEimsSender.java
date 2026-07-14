package io.shinhanlife.axhub.biz.mcp.adapter.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;

import io.shinhanlife.axhub.biz.mcp.tool.config.GlowCommunicationProperties;

/**
 * @package io.shinhanlife.axhub.biz.mcp.adapter.sender
 * @className HttpEimsSender
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
@Component
public class HttpEimsSender implements EimsSender {

    private final RestClient restClient;
    private final String eimsUrl;
    private final GlowCommunicationProperties glowProps;

    public HttpEimsSender(GlowCommunicationProperties glowProps) {
        this.glowProps = glowProps;
        // yml의 대내 MCI host, port, uri를 조합하여 EIMS 호출 주소 생성
        this.eimsUrl = glowProps.getMci().getHost() + ":" + glowProps.getMci().getPort() + glowProps.getMci().getUri();

        /*
        *********************************************** 중요 **************************************************
        this.restClient = RestClient.create();
        보통 금융권(신한라이프 등 은행/보험사)의 내부 레거시 시스템이나 MCI(Message Channel Integration) 솔루션은 HTTP/2를 기본으로 지원하지 않는 경우가 훨씬 많습니다.
        *********************************************** 중요 **************************************************
         */

        // HTTP/2 통신 시 Stream Cancelled(RST_STREAM) 에러 방지를 위해 HTTP/1.1 전용 Factory 사용
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String send(String interfaceId, String payload) {
        log.info(" [HTTP 모드] EIMS API 호출 중... URL: {}", eimsUrl);
        
        // EIMS가 요구하는 JSON 포맷으로 래핑해서 전송 (EIMS 규격에 따라 수정 가능)
        Map<String, String> requestBody = Map.of(
            "interfaceId", interfaceId,
            "data", payload
        );

        //  4번 항목 적용: MDC에 저장된 traceId를 추출하여 HTTP Header(X-Trace-Id)로 전파
        String traceId = MDC.get("traceId");
        if (traceId == null) traceId = "SYSTEM-GENERATED-" + UUID.randomUUID().toString();

        return restClient.post()
                .uri(eimsUrl)
                .header("X-Trace-Id", traceId)
                .header("X-Shinhan-Global-ID", traceId)
                .body(requestBody)
                .retrieve()
                .body(String.class); // 응답 결과를 String으로 받음
    }
}