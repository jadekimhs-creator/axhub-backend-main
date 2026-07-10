package io.shinhanlife.axhub.biz.mcp.tool.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.dto
 * @className ToolMetadata
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolMetadata {
    private String toolName;
    private String description;
    private Map<String, Object> parametersSchema;
    private Map<String, String> actionPrompts;
    private String domainGroup;
    private String endpoint;
    private String podUrl;
    private String integrationType;
    private String mciServiceId;


    @Builder.Default
    private Boolean visible = true;

    @Builder.Default
    private Boolean isRegistered = true;

    @Builder.Default
    private Boolean requiresApproval = false;

    public boolean isVisible() {
        return visible != null ? visible : true;
    }

    public boolean isRegistered() {
        return isRegistered != null ? isRegistered : true;
    }
    
    public void setRegistered(Boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    public boolean isRequiresApproval() {
        return requiresApproval != null ? requiresApproval : false;
    }

}