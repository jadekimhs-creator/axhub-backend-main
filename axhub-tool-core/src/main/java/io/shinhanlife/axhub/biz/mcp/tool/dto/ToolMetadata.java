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


    private boolean visible = true;


}
