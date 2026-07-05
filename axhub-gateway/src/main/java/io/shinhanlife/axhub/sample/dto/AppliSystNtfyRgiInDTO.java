package io.shinhanlife.axhub.sample.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliSystNtfyRgiInDTO {
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
