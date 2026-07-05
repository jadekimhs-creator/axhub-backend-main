package io.shinhanlife.axhub.biz.mcp.adapter.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class JspFormEimsSender implements EimsSender {

    private final RestClient restClient;
    private final String jspUrl;

    public JspFormEimsSender(@Value("${eims.jsp.form.url}") String jspUrl) {
        this.jspUrl = jspUrl;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String send(String interfaceId, String payload) {
        log.info("🌐 [JSP Form 모드] 레거시 폼 데이터 전송 중... URL: {}", jspUrl);

        // 1. Form Data 조립 (HTML <form> 태그 전송과 동일한 효과)
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("interfaceId", interfaceId);
        formData.add("data", payload);

        // 2. HTTP 전송 (Content-Type: application/x-www-form-urlencoded)
        String rawResponse = restClient.post()
                .uri(jspUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(String.class);

        // 3. JSP 특유의 앞뒤 공백 및 엔터 제거
        return rawResponse != null ? rawResponse.trim() : "";
    }
}
