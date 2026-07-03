package io.shinhanlife.axhub.biz.mcp.tool.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ToolFunction {
    private String name;
    private String description;
    private String interfaceId;
    private String parameterSchema;
}
