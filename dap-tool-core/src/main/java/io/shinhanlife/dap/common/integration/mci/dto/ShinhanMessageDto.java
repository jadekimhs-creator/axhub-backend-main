package io.shinhanlife.dap.common.integration.mci.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @package io.shinhanlife.dap.common.integration.mci.dto
 * @className ShinhanMessageDto
 * @description AX HUB 시스템 처리 클래스 - MCI 전문 메시지부
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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShinhanMessageDto {

    private MsgHddvValu msgHddvValu; // 메시지헤더부값
    private MsgDtdvValu msgDtdvValu; // 메시지데이터부값

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MsgHddvValu {
        private String msgTnsmTypeCd; // 메시지전송유형코드
        private Integer msdvLencn;    // 메시지부길이
        private Integer msgRpttCc;    // 메시지반복건수
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MsgDtdvValu {
        private String msgCd;         // 메시지코드
        private String msgPrnAttrCd;  // 메시지출력속성코드
        private String msgCt;         // 메시지내용
        private String anxMsgCt;      // 부가메시지내용
    }
}
