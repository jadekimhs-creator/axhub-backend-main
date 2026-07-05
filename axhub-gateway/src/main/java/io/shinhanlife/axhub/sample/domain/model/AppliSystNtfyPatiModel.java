package io.shinhanlife.axhub.sample.domain.model;

import io.shinhanlife.glow.db.dto.AuditInfo;
import lombok.*;

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
