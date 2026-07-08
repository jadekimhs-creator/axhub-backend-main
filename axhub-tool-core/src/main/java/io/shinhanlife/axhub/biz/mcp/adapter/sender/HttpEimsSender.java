package io.shinhanlife.axhub.biz.mcp.adapter.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

import io.shinhanlife.axhub.biz.mcp.tool.config.GlowCommunicationProperties;

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
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(glowProps.getMci().getConnectionTimeout() * 1000);
        factory.setReadTimeout(glowProps.getMci().getReadTimeout() * 1000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String send(String interfaceId, String payload) {
        log.info("🌐 [HTTP 모드] EIMS API 호출 중... URL: {}", eimsUrl);
        
        // EIMS가 요구하는 JSON 포맷으로 래핑해서 전송 (EIMS 규격에 따라 수정 가능)
        Map<String, String> requestBody = Map.of(
            "interfaceId", interfaceId,
            "data", payload
        );

        // 📊 4번 항목 적용: MDC에 저장된 traceId를 추출하여 HTTP Header(X-Trace-Id)로 전파
        String traceId = org.slf4j.MDC.get("traceId");
        if (traceId == null) traceId = "SYSTEM-GENERATED-" + java.util.UUID.randomUUID().toString();

        return restClient.post()
                .uri(eimsUrl)
                .header("X-Trace-Id", traceId)
                .header("X-Shinhan-Global-ID", traceId)
                .body(requestBody)
                .retrieve()
                .body(String.class); // 응답 결과를 String으로 받음
    }
}
