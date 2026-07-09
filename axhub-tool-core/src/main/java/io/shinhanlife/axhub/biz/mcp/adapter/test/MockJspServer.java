package io.shinhanlife.axhub.biz.mcp.adapter.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.adapter.test
 * @className MockJspServer
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
@RequestMapping("/mock")
public class MockJspServer {

    // ==========================================
    // 1. Form Data 방식 테스트 수신부 (jsp-form)
    // ==========================================
    @PostMapping(value = "/jsp-form", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String mockJspFormReceiver(
            @RequestParam("interfaceId") String interfaceId,
            @RequestParam("data") String data) {

        log.info(" [가짜 JSP 서버] 폼 데이터(Form) 수신 완료!");
        log.info(" 파라미터 파싱 확인 - ID: {}, DATA: [{}]", interfaceId, data);

        // 실제 JSP 서버처럼 앞뒤에 의미 없는 줄바꿈(엔터)과 공백을 잔뜩 넣어서 리턴합니다.
        return "\n\n   SUCCESS_FROM_MOCK_JSP_FORM   \n\n";
    }

    // ==========================================
    // 2. JSON 방식 테스트 수신부 (jsp-json)
    // ==========================================
    @PostMapping(value = "/jsp-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String mockJspJsonReceiver(@RequestBody Map<String, String> request) {

        String interfaceId = request.get("interfaceId");
        String data = request.get("data");

        log.info(" [가짜 JSP 서버] 제이슨(JSON) 수신 완료!");
        log.info(" JSON 파싱 확인 - ID: {}, DATA: [{}]", interfaceId, data);

        // 여기도 마찬가지로 쓰레기 여백을 넣어줍니다.
        return "\n\n   SUCCESS_FROM_MOCK_JSP_JSON   \n\n";
    }
}