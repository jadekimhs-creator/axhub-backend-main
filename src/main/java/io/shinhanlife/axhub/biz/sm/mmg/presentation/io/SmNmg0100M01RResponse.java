package io.shinhanlife.axhub.biz.sm.mmg.presentation.io;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmNmg0100M01RResponse {

    private Integer id;
    private Integer parentMenuId;
    private String name;
    private String path;
    private Meta meta;
    private List<SmNmg0100M01RResponse> children;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        private String title;
        private String icon;
        private String link;
        private Integer orderSeq;

        private boolean hide;
        private boolean hideTab;
        private boolean iframe;
        private boolean keepAlive;
        private boolean active;

        @JsonProperty("isHide")
        public boolean isHide() { return hide; }

        @JsonProperty("isHide")
        public void setHide(boolean hide) { this.hide = hide; }

        @JsonProperty("isHideTab")
        public boolean isHideTab() { return hideTab; }

        @JsonProperty("isHideTab")
        public void setHideTab(boolean hideTab) { this.hideTab = hideTab; }

        @JsonProperty("isIframe")
        public boolean isIframe() { return iframe; }

        @JsonProperty("isIframe")
        public void setIframe(boolean iframe) { this.iframe = iframe; }

        @JsonProperty("keepAlive")
        public boolean isKeepAlive() { return keepAlive; }

        @JsonProperty("isActive")
        public boolean isActive() { return active; }

        @JsonProperty("isActive")
        public void setActive(boolean active) { this.active = active; }
    }
}
