package io.shinhanlife.axhub.biz.mcp.adapter.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.adapter.sender
 * @className JspJsonEimsSender
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
public class JspJsonEimsSender implements EimsSender {

    private final RestClient restClient;
    private final String jspUrl;

    //  여기서 eims.jsp.json.url 딱 하나만 깔끔하게 받아옵니다!
    public JspJsonEimsSender(@Value("${eims.jsp.json.url}") String jspUrl) {
        this.jspUrl = jspUrl;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String send(String interfaceId, String payload) {
        log.info("🌐 [JSP JSON 모드] JSON 페이로드 전송 중... URL: {}", jspUrl);

        // 1. JSON 객체로 조립 (스프링이 알아서 JSON String으로 변환해 줌)
        Map<String, String> jsonBody = Map.of(
                "interfaceId", interfaceId,
                "data", payload
        );

        // 2. HTTP 전송 (Content-Type: application/json)
        String rawResponse = restClient.post()
                .uri(jspUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .retrieve()
                .body(String.class);

        // 3. JSP 응답 정제
        return rawResponse != null ? rawResponse.trim() : "";
    }
}