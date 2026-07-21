package io.shinhanlife.dap.mcc.other.service;

import io.shinhanlife.dap.common.integration.mci.component.AxhubMciComponent;
import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import io.shinhanlife.dap.mcc.dto.Onnba3011ReqDto;
import io.shinhanlife.glow.communication.dto.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String INTERFACE_CODE_3011 = "CLCNNB00001";

    // Glow 기반의 AxhubMciComponent 주입
    private final AxhubMciComponent mci;

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
            mappingId = INTERFACE_CODE_3011
    )
    public Object callOnnba3011(Onnba3011ReqDto req) {
        log.info("[MCI Tool] 보종By가입설계한도계산조회 요청 수신.");

        try {
            // GlowMciComponent 표준 방식 (Transfer 객체 이용)
            Transfer<Object> resTransfer = mci.callTo(
                    INTERFACE_CODE_3011, 
                    null, // rcvSvcId
                    req, 
                    Object.class
            );
            
            log.info("[MCI Tool] Glow 기반 MCI 연동 성공.");
            
            // 결과 반환 (실제로는 resTransfer.getBody() 리턴)
            return resTransfer.getBody() != null ? resTransfer.getBody() : "{\"status\":\"SUCCESS\", \"message\":\"GlowMciComponent 통신 완료\"}";

        } catch (Exception e) {
            log.error("[MCI Tool] MCI 연동 중 오류 발생: {}", e.getMessage(), e);
            return "{\"status\":\"ERROR\", \"message\":\"MCI 통신 실패: " + e.getMessage() + "\"}";
        }
    }
}
