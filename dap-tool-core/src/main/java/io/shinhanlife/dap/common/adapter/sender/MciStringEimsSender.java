package io.shinhanlife.dap.common.adapter.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestClient;

/**
 * @package io.shinhanlife.dap.common.adapter.sender
 * @className MciStringEimsSender
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
@Service("mciStringEimsSender")
public class MciStringEimsSender implements EimsSender {

    private final RestClient restClient;
    private final String mciUrl;

    public MciStringEimsSender(@Value("${eims.mcistring.url}") String mciUrl) {
        this.mciUrl = mciUrl;
        this.restClient = RestClient.create();
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
    public String send(String interfaceId, String payload) throws Exception {
        StopWatch stopWatch = new StopWatch(); stopWatch.start();

        try {
            log.info(" [ESB 어댑터(String)] 전송 준비 완료 - RestClient 호출 시작 (Interface: {})", interfaceId);

            String response = restClient.post()
                    .uri(mciUrl)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(payload != null ? payload : "")
                    .retrieve()
                    .body(String.class);

            log.info(" [ESB 어댑터(String)] 응답 수신 완료: {}", response);
            return response != null ? response : "";

        } finally {
            stopWatch.stop();
            log.info(" [SLA 모니터링 - MCI(String)] 소요시간: {} ms", stopWatch.getTotalTimeMillis());
        }
    }
}
