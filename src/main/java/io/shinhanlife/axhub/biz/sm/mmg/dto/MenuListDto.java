package io.shinhanlife.axhub.biz.sm.mmg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MenuDto는 Vue의 MenuListType에 맞춘 DTO입니다.
 * 트리 구조를 지원하며, meta 객체 포함.
 * @Data: Lombok을 통해 getter, setter 등을 자동 생성.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuListDto {
    private Integer id;
    private Integer parentMenuId;
    private String name;
    private String path;
    private Meta meta;
    private List<MenuListDto> children;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        private String title;
        private String icon;
        private boolean showBadge;
        private String showTextBadge;
        private boolean hide;
        private boolean hideTab;
        private String link;
        private boolean iframe;
        private boolean keepAlive;

        // 메뉴 표시 순서
        private Integer orderSeq;
        // 활성 여부
        private boolean isActive;

        @JsonProperty("isHide")
        public boolean isHide() {
            return hide;
        }

        @JsonProperty("isHide")
        public void setHide(boolean hide) {
            this.hide = hide;
        }

        @JsonProperty("isHideTab")
        public boolean isHideTab() {
            return hideTab;
        }

        @JsonProperty("isHideTab")
        public void setHideTab(boolean hideTab) {
            this.hideTab = hideTab;
        }

        @JsonProperty("isIframe")
        public boolean isIframe() {
            return iframe;
        }

        @JsonProperty("isIframe")
        public void setIframe(boolean iframe) {
            this.iframe = iframe;
        }

        @JsonProperty("isActive")
        public boolean isActive() {
            return isActive;
        }

        @JsonProperty("isActive")
        public void setActive(boolean isActive) {
            this.isActive = isActive;
        }
    }
}