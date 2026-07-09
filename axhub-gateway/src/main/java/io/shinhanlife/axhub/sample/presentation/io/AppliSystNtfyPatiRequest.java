package io.shinhanlife.axhub.sample.presentation.io;

import lombok.*;

/**
 * @package io.shinhanlife.axhub.sample.presentation.io
 * @className AppliSystNtfyPatiRequest
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
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AppliSystNtfyPatiRequest {
    private String appliSystNtfyPatiId;
    private String appliSystNtfyKdCd;
    private String appliDutjCd;
    private String appliDutjPrjcCd;
    private String ntfyMsgCt;
    private String ntfyOccDt;
    private String ntfyCfmDt;
    private String ntfyTrgtPrafNo;
    private String shmsPmlYn;
    private String ntfyCfmYn;
}