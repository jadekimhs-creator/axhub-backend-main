package io.shinhanlife.axhub.biz.mcp.adapter.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestClient;

/**
 * @package io.shinhanlife.axhub.biz.mcp.adapter.sender
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
    }

    @Override
    public String send(String interfaceId, String payload) throws Exception {
        StopWatch stopWatch = new StopWatch(); stopWatch.start();

        try {
            log.info("🌐 [ESB 어댑터(String)] 전송 준비 완료 - RestClient 호출 시작 (Interface: {})", interfaceId);

            String response = restClient.post()
                    .uri(mciUrl)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(payload != null ? payload : "")
                    .retrieve()
                    .body(String.class);

            log.info("🌐 [ESB 어댑터(String)] 응답 수신 완료: {}", response);
            return response != null ? response : "";

        } finally {
            stopWatch.stop();
            log.info("📊 [SLA 모니터링 - MCI(String)] 소요시간: {} ms", stopWatch.getTotalTimeMillis());
        }
    }
}
