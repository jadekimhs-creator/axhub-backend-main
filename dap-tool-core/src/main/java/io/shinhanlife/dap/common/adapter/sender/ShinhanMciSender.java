package io.shinhanlife.dap.common.adapter.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dap.common.integration.mci.config.ShinhanIntegrationProperties;
import io.shinhanlife.dap.common.integration.mci.dto.MciRequestWrapper;
import io.shinhanlife.dap.common.integration.mci.dto.ShinhanCommonHeaderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @package io.shinhanlife.dap.common.adapter.sender
 * @className ShinhanMciSender
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@Service
@EnableConfigurationProperties(ShinhanIntegrationProperties.class)
public class ShinhanMciSender {

    private final ObjectMapper jsonMapper;
    private final RestClient restClient;
    private final ShinhanIntegrationProperties properties;
    private final AtomicInteger sequenceGenerator = new AtomicInteger(1);

    public ShinhanMciSender(ObjectMapper jsonMapper, ShinhanIntegrationProperties properties) {
        this.jsonMapper = jsonMapper;
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public <T> String send(String targetUrl, MciRequestWrapper<T> requestWrapper) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            ShinhanCommonHeaderDto header = requestWrapper.getTgrmCmnnhddValu();
            if (header == null) {
                header = new ShinhanCommonHeaderDto();
                requestWrapper.setTgrmCmnnhddValu(header);
            }

            // 필수 헤더 자동 세팅 로직
            header.setEnvrTypeCd(properties.getEnvrTypeCd());
            header.setReqRspnScCd("S"); // S: 요청
            header.setAppliDutjCd("DAP"); // 어플리케이션업무코드

            if (header.getGlbId() == null || header.getGlbId().isEmpty()) {
                header.setGlbId(generateGlbId());
            }

            int currentSeq = sequenceGenerator.getAndIncrement();
            header.setPgrsSriaNo(String.format("%03d", currentSeq)); // 3자리

            if (header.getReqTgrmTnsmDtptDt() == null) {
                header.setReqTgrmTnsmDtptDt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
            }

            String jsonPayload = jsonMapper.writeValueAsString(requestWrapper);

            log.info(" [신한 통합 MCI 어댑터] 전송 준비 완료 - URL: {}", targetUrl);
            log.debug(" [신한 통합 MCI 어댑터] 요청 Payload: {}", jsonPayload);

            String responseJson = restClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonPayload)
                    .retrieve()
                    .body(String.class);

            log.info(" [신한 통합 MCI 어댑터] 응답 수신 완료");
            log.debug(" [신한 통합 MCI 어댑터] 응답 Payload: {}", responseJson);

            return responseJson;

        } finally {
            stopWatch.stop();
            log.info(" [SLA 모니터링 - MCI 연계] 소요시간: {} ms", stopWatch.getTotalTimeMillis());
        }
    }

    private String generateGlbId() {
        // 전사공통키 (37 Byte)
        // 전문생성상세일시(17) + 전문생성시스템명(9) + 어플리케이션업무코드(3) + 전문세션번호(8)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")); // 17 byte
        String systemName = String.format("%-9s", "AXHUB"); // 9 byte, left-aligned padded with spaces
        String appCode = "DAP"; // 3 byte
        String sessionNo = UUID.randomUUID().toString().substring(0, 8).toUpperCase(); // 8 byte
        return timestamp + systemName + appCode + sessionNo;
    }
}
