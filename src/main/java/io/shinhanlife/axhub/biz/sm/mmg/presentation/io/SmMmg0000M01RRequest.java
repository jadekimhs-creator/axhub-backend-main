package io.shinhanlife.axhub.biz.sm.mmg.presentation.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SmMmg0000M01RRequest {

    public SmMmg0000M01RRequest() {
        this.isActive = "Y";
    }

    private String isActive; // 기본 활성 메뉴만
    private String path; // 라우트 경로
    private String title; // 기본 활성 메뉴만
}
