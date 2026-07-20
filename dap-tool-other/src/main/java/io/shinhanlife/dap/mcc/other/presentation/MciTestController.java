package io.shinhanlife.dap.mcc.other.presentation;

import io.shinhanlife.dap.common.adapter.sender.ShinhanMciSender;
import io.shinhanlife.dap.common.integration.mci.dto.MciRequestWrapper;
import io.shinhanlife.dap.common.integration.mci.dto.ShinhanCommonHeaderDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @package io.shinhanlife.dap.mcc.other.presentation
 * @className MciTestController
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
@RequestMapping("/api/test/mci")
@RequiredArgsConstructor
public class MciTestController {

    private final ShinhanMciSender shinhanMciSender;

    @Data
    public static class SampleBizData {
        private String customerId;
        private String amount;
        private String message;
    }

    @PostMapping("/send")
    public String testSendMci(HttpServletRequest request, @RequestBody SampleBizData bizData) {
        try {
            MciRequestWrapper<SampleBizData> wrapper = new MciRequestWrapper<>();
            
            ShinhanCommonHeaderDto header = new ShinhanCommonHeaderDto();
            header.setItrIfId("NCSBACO00001");
            header.setRcvSvcId("OBACA9040");
            wrapper.setTgrmCmnnhddValu(header);
            
            wrapper.setBody(bizData);

            String hostUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String targetUrl = hostUrl + "/api/mock/esb/api"; // 로컬 Mock 서버 URL 호출
            
            return shinhanMciSender.send(targetUrl, wrapper);
            
        } catch (Exception e) {
            return "MCI 전송 실패: " + e.getMessage();
        }
    }
}
