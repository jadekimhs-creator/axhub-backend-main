package io.shinhanlife.axhub.biz.mcp.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {"io.shinhanlife.axhub.biz.mcp.gateway", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"})
@ConfigurationPropertiesScan(basePackages = {"io.shinhanlife.axhub.biz.mcp.gateway", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"})
@EnableCaching
public class AxHubGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(AxHubGatewayApplication.class, args);
    }
}