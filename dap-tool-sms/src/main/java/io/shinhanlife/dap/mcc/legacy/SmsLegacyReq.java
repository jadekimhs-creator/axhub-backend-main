package io.shinhanlife.dap.mcc.legacy;


/**
 * @package io.shinhanlife.dap.mcc.sms.dto
 * @className SmsLegacyReq
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
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SmsLegacyReq {
    /**
     * EAI 시스템이 요구하는 수신자 번호 파라미터명
     */
    private String phone;

    /**
     * EAI 시스템이 요구하는 메시지 내용 파라미터명
     */
    private String content;
}
