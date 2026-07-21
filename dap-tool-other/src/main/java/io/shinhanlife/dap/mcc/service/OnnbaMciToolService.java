package io.shinhanlife.dap.mcc.service;

import io.shinhanlife.dap.common.adapter.sender.ShinhanMciSender;
import io.shinhanlife.dap.common.integration.mci.dto.MciRequestWrapper;
import io.shinhanlife.dap.common.integration.mci.dto.ShinhanCommonHeaderDto;
import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.shinhanlife.dap.mcc.dto.Onnba3011ReqDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @package io.shinhanlife.dap.mcc.service
 * @className OnnbaMciToolService
 * @description 보종By가입설계한도계산조회 MCI 연동 툴
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
@Service
@RequiredArgsConstructor
@McpTool(routingType = "MCI", categoryKey = "other") 
public class OnnbaMciToolService {

    // 프레임워크에서 제공하는 공통 MCI 발송 어댑터 주입
    private final ShinhanMciSender shinhanMciSender;

    @Value("${shinhan.integration.mci.default-url:http://localhost:8081/api/mock/esb/api}")
    private String mciTargetUrl;



    /**
     * AI Agent가 호출하게 될 메서드입니다.
     * @McpFunction 어노테이션 하나로 AI 도구로 자동 노출 및 라우팅됩니다.
     */
    @McpFunction(
            register = true, 
            name = "calculate_subscription_limit",
            displayName = "보종By가입설계한도계산조회",
            description = "MCI 연동을 통해 보종By가입설계한도계산조회를 수행합니다.",
            prompt = "가입설계 한도를 계산하고 조회해줘.",
            mappingId = "CLCNNB00001"
    )
    public Object callOnnba3011(Onnba3011ReqDto req) {
        log.info("[MCI Tool] 보종By가입설계한도계산조회 요청 수신.");

        try {
            // 1. MCI 전문 통신을 위한 통합 Wrapper 객체 생성
            MciRequestWrapper<Onnba3011ReqDto> wrapper = new MciRequestWrapper<>();
            
            // 2. 공통 헤더 세팅
            ShinhanCommonHeaderDto header = new ShinhanCommonHeaderDto();
            header.setItrIfId("CLCNNB00001"); 
            wrapper.setTgrmCmnnhddValu(header);
            
            // 3. 비즈니스 데이터(Body) 세팅
            wrapper.setBody(req);

            // 4. ShinhanMciSender를 통해 통합 JSON 연동 수행 및 응답 수신
            log.info("[MCI Tool] ShinhanMciSender 연동 시작... (URL: {})", mciTargetUrl);
            String responseJson = shinhanMciSender.send(mciTargetUrl, wrapper);
            
            log.info("[MCI Tool] MCI 연동 성공.");
            
            // 결과 반환
            return responseJson;

        } catch (Exception e) {
            log.error("[MCI Tool] MCI 연동 중 오류 발생: {}", e.getMessage(), e);
            return "{\"status\":\"ERROR\", \"message\":\"MCI 통신 실패: " + e.getMessage() + "\"}";
        }
    }
}
