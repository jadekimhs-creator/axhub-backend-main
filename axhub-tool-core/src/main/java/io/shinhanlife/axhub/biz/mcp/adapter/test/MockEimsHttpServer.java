package io.shinhanlife.axhub.biz.mcp.adapter.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.adapter.test
 * @className MockEimsHttpServer
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
@RestController
@RequestMapping("/api")
public class MockEimsHttpServer {

    private final ObjectMapper objectMapper;

    public MockEimsHttpServer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping("/gateway")
    public Map<String, Object> mockEimsReceiver(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestBody Map<String, Object> request) {
            
        String interfaceId = (String) request.get("interfaceId");
        String data = (String) request.get("data");

        log.info(" [가짜 EIMS 서버] HTTP 요청 수신 완료!");
        log.info(" 수신된 Trace-ID (거래고유번호): {}", traceId != null ? traceId : "없음");
        log.info(" 인터페이스ID: {}, 데이터: [{}]", interfaceId, data);

        // No-Code Mock 응답 생성 (JSON 설정 파일 기반)
        try {
            ClassPathResource resource = new ClassPathResource("mock-responses.json");
            Map<String, Object> mockDataMap = objectMapper.readValue(resource.getInputStream(), Map.class);

            if (mockDataMap.containsKey(interfaceId)) {
                return (Map<String, Object>) mockDataMap.get(interfaceId);
            }
        } catch (Exception e) {
            log.warn("JSON 파싱 에러 또는 mock 파일 읽기 실패 (기본 응답 반환)", e);
        }

        // 기본 응답
        return Map.of(
            "status", "404",
            "message", "MOCK 데이터가 정의되지 않았습니다.",
            "receivedLength", data.length()
        );
    }

    @PostMapping("/mock/esb/api")
    public String mockEsbReceiver(@RequestBody String xmlPayload) {
        log.info(" [가짜 ESB 서버] MCI/ESB 요청 수신 완료!");
        log.info(" 수신된 XML 전문: {}", xmlPayload);
        
        // MciEimsSender가 기대하는 JSON 변환용 XML 포맷 응답
        return "<Response><status>SUCCESS</status><message>MOCK_MCI_EIMS_RECEIVE_SUCCESS</message><data><info>정상 처리되었습니다.</info></data></Response>";
    }
}