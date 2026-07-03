package io.shinhanlife.axhub.biz.mcp.tool.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ToolConfig {
    private String name;
    private String description;
    private String path;
    private String group;
    
    // 추가: 레거시 통신용 동적 설정
    private String routingType;
    private List<ToolFunction> functions;
}
