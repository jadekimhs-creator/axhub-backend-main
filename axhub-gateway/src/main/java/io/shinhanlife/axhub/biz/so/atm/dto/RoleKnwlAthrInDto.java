package io.shinhanlife.axhub.biz.so.atm.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleKnwlAthrInDto {
    private String roleKnwlAthrId;
    private String systId;
    private String roleNo;
    private String knwlId;
    private String puseYn;

    private Date   systRgiDt;
    private String systRgiPrafNo;
    private String systRgiOgnzNo;
    private String systRgiSystCd;
    private String systRgiPrgrId;
    private Date   systChgDt;
    private String systChgPrafNo;
    private String systChgOgnzNo;
    private String systChgSystCd;
    private String systChgPrgrId;
}
