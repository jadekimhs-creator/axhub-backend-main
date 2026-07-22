package io.shinhanlife.dap.mcc.legacy;

import lombok.Data;

/**
 * @package io.shinhanlife.dap.mcc.legacy
 * @className SearchHrLegacyReq
 * @description AX HUB 시스템 처리 클래스
 * @author user
 * @create 2026.07.22
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.07.22  user    최초생성
 *
 * </pre>
 */
@Data
public class SearchHrLegacyReq {
    /**
     * EAI 시스템이 요구하는 수신자 번호 파라미터명
     */
    private String phone;

    /**
     * EAI 시스템이 요구하는 메시지 내용 파라미터명
     */
    private String content;
}
