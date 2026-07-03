package io.shinhanlife.axhub.biz.sm.mmg.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class MenuSaveInDto {

    private Integer menuId;
    private Integer parentMenuId;
    private String name;
    private String path;
    private String title;
    private String icon;
    private String isHide;
    private String isHideTab;
    private String link;
    private String isIframe;
    private String keepAlive;
    private Integer orderSeq;
    private String isActive;

    private Date systRgiDt;
    private String systRgiPrafNo;
    private String systRgiOgnzNo;
    private String systRgiSystCd;
    private String systRgiPrgrId;

    private Date systChgDt;
    private String systChgPrafNo;
    private String systChgOgnzNo;
    private String systChgSystCd;
    private String systChgPrgrId;
}