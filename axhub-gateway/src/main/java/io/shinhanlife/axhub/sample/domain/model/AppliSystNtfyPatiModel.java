package io.shinhanlife.axhub.sample.domain.model;

import io.shinhanlife.glow.db.dto.AuditInfo;
import lombok.*;

/**
 * @package io.shinhanlife.axhub.sample.domain.model
 * @className AppliSystNtfyPatiModel
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
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppliSystNtfyPatiModel extends AuditInfo {
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