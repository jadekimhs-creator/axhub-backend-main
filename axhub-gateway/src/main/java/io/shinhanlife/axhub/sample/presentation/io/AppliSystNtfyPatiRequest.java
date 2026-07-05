package io.shinhanlife.axhub.sample.presentation.io;

import lombok.*;

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
