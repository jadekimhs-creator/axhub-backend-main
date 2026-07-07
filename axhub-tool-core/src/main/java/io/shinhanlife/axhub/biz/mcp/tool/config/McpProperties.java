package io.shinhanlife.axhub.biz.mcp.tool.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    private Map<String, FunctionProp> functions;

    @Data
    public static class FunctionProp {
        private String description;
        private String prompt;
        private String mappingId;
        private Boolean register;
    }
}
