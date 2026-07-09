package io.shinhanlife.axhub.biz.mcp.gateway.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.gateway.controller
 * @className MciMockController
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
@RestController
public class MciMockController {

    @PostMapping("/mciSend")
    public Map<String, Object> mciSend() {
        return Map.of(
            "resCode", "0000",
            "resMsg", "정상 처리되었습니다.",
            "data", Map.of(
                "status", "SUCCESS",
                "mock", true,
                "protocol", "HTTP/2 (h2c)",
                "detail", "가짜 응답 서버(HTTP/2 WebFlux)에서 내려주는 대내(MCI) 샘플 데이터입니다."
            )
        );
    }

    @PostMapping("/extmci")
    public Map<String, Object> extmci() {
        return Map.of(
            "resCode", "0000",
            "resMsg", "정상 처리되었습니다.",
            "data", Map.of(
                "status", "SUCCESS",
                "mock", true,
                "protocol", "HTTP/2 (h2c)",
                "detail", "가짜 응답 서버(HTTP/2 WebFlux)에서 내려주는 대외(EXTMCI) 샘플 데이터입니다."
            )
        );
    }
}