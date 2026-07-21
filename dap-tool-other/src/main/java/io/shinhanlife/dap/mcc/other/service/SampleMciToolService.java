package io.shinhanlife.dap.mcc.other.service;

import io.shinhanlife.dap.common.adapter.sender.ShinhanMciSender;
import io.shinhanlife.dap.common.integration.mci.dto.MciRequestWrapper;
import io.shinhanlife.dap.common.integration.mci.dto.ShinhanCommonHeaderDto;
import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * @package io.shinhanlife.dap.mcc.service
 * @className SampleMciToolService
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
@RequiredArgsConstructor
@McpTool(routingType = "MCI", categoryKey = "other") // MCP Tool 등록 어노테이션
public class SampleMciToolService {

    // EIMS/MCI 발송을 담당하는 Sender 주입
    private final ShinhanMciSender shinhanMciSender;

    @Value("${shinhan.integration.mci.default-url:http://localhost:8081/api/mock/esb/api}")
    private String mciTargetUrl;

    // AI가 인식할 파라미터 DTO
    @Data
    public static class SampleMciReqDto {
        private String customerId;
        private String inquiryType;
    }

    /**
     * AI Agent가 호출하게 될 메서드입니다.
     */
    @McpFunction(register = false, name = "inquiry_customer_mci",
            displayName = "고객 정보 조회 (MCI)",
            description = "MCI 연동을 통해 고객의 상세 정보를 조회합니다.",
            prompt = "고객 정보를 조회해줘.",
            mappingId = "CUST_INQ_001"
    )
    public Object inquiryCustomerInfo(SampleMciReqDto req) {
        log.info("[MCI Tool] 고객 정보 조회 요청 수신. 고객ID: {}", req.getCustomerId());

        try {
            // 1. MCI 전문 통신을 위한 Wrapper 객체 생성
            MciRequestWrapper<SampleMciReqDto> wrapper = new MciRequestWrapper<>();
            
            // 2. 공통 헤더 세팅 (인터페이스 ID, 서비스 ID 등 업무에 맞게 설정)
            ShinhanCommonHeaderDto header = new ShinhanCommonHeaderDto();
            header.setItrIfId("CUST_INQ_001");
            header.setRcvSvcId("INQ9040");
            wrapper.setTgrmCmnnhddValu(header);
            
            // 3. 비즈니스 데이터(Body) 세팅
            wrapper.setBody(req);

            // 4. ShinhanMciSender를 통해 실제 연동 수행 및 응답 수신
            log.info("[MCI Tool] ShinhanMciSender 연동 시작... (URL: {})", mciTargetUrl);
            String responseJson = shinhanMciSender.send(mciTargetUrl, wrapper);
            
            log.info("[MCI Tool] MCI 연동 성공. 응답 수신 완료");
            
            // 결과 반환 (이 문자열은 JSON 파싱되거나 그대로 AI에게 전달됩니다)
            return responseJson;

        } catch (Exception e) {
            log.error("[MCI Tool] MCI 연동 중 오류 발생: {}", e.getMessage(), e);
            return "{\"status\":\"ERROR\", \"message\":\"MCI 통신 실패: " + e.getMessage() + "\"}";
        }
    }
}
