package io.shinhanlife.axhub.biz.sm.mmg.presentation.io;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmNmg0100M01SRequest {

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
}