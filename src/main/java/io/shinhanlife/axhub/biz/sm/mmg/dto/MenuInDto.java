package io.shinhanlife.axhub.biz.sm.mmg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class MenuInDto {

    public MenuInDto() {
        setIsActive("Y");
    }

    private String isActive; // 기본 활성 메뉴만
    private String path; // 라우트 경로
    private String title; // 기본 활성 메뉴만

}
