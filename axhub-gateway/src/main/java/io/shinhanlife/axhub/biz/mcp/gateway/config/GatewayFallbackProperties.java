package io.shinhanlife.axhub.biz.mcp.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "mcp.gateway.fallback")
public class GatewayFallbackProperties {
    private Map<String, String> routes = new HashMap<>();
    private String defaultUrl;
}
