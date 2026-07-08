package io.shinhanlife.axhub.biz.mcp.tool.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

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
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getParametersSchema() { return parametersSchema; }
    public void setParametersSchema(Map<String, Object> parametersSchema) { this.parametersSchema = parametersSchema; }

    public Map<String, String> getActionPrompts() { return actionPrompts; }
    public void setActionPrompts(Map<String, String> actionPrompts) { this.actionPrompts = actionPrompts; }

    public String getDomainGroup() { return domainGroup; }
    public void setDomainGroup(String domainGroup) { this.domainGroup = domainGroup; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getPodUrl() { return podUrl; }
    public void setPodUrl(String podUrl) { this.podUrl = podUrl; }

    public String getIntegrationType() { return integrationType; }
    public void setIntegrationType(String integrationType) { this.integrationType = integrationType; }

    public String getMciServiceId() { return mciServiceId; }
    public void setMciServiceId(String mciServiceId) { this.mciServiceId = mciServiceId; }

    private boolean visible = true;

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
