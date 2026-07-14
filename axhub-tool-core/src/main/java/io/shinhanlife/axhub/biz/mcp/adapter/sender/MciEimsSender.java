package io.shinhanlife.axhub.biz.mcp.adapter.sender;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestClient;

/**
 * @package io.shinhanlife.axhub.biz.mcp.adapter.sender
 * @className MciEimsSender
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
@Service("mciEimsSender")
public class MciEimsSender implements EimsSender {

    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;
    private final RestClient restClient; // Spring Boot 3.2+ 최신 HTTP 클라이언트
    private final String mciUrl;

    public MciEimsSender(ObjectMapper jsonMapper, XmlMapper xmlMapper, @Value("${eims.mci.url}") String mciUrl) {
        this.jsonMapper = jsonMapper;
        this.xmlMapper = xmlMapper;
        this.mciUrl = mciUrl;
        this.restClient = RestClient.create(); // 클라이언트 초기화

        /*
        *********************************************** 중요 **************************************************
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
        보통 금융권(신한라이프 등 은행/보험사)의 내부 레거시 시스템이나 MCI(Message Channel Integration) 솔루션은 HTTP/2를 기본으로 지원하지 않는 경우가 훨씬 많습니다.
        *********************************************** 중요 **************************************************
         */
    }

    @Override
    public String send(String interfaceId, String jsonPayload) throws Exception {
        StopWatch stopWatch = new StopWatch(); stopWatch.start();

        try {
            JsonNode jsonNode = jsonMapper.readTree(jsonPayload);
            String xmlData = xmlMapper.writer().withRootName("Body").writeValueAsString(jsonNode);
            String esbStandardXml = wrapWithEsbHeader(interfaceId, xmlData);

            log.info(" [ESB 어댑터] 전송 준비 완료 - RestClient 호출 시작");

            String responseXml = restClient.post()
                    .uri(mciUrl)
                    .contentType(MediaType.APPLICATION_XML)
                    .body(esbStandardXml)
                    .retrieve()
                    .body(String.class);

            log.info(" [ESB 어댑터] 응답 수신 완료: {}", responseXml);

            JsonNode responseNode = xmlMapper.readTree(responseXml);
            return jsonMapper.writeValueAsString(responseNode);

        } finally {
            stopWatch.stop();
            log.info(" [SLA 모니터링 - MCI] 소요시간: {} ms", stopWatch.getTotalTimeMillis());
        }
    }

    private String wrapWithEsbHeader(String interfaceId, String xmlData) {
        return String.format("<EsbMessage><Header><InterfaceId>%s</InterfaceId></Header><Body>%s</Body></EsbMessage>", interfaceId, xmlData);
    }
}