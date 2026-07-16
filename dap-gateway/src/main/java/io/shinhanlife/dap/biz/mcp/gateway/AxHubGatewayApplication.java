package io.shinhanlife.dap.biz.mcp.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {"io.shinhanlife.dap.biz.mcp.gateway", "io.shinhanlife.dap.common.mcp", "io.shinhanlife.dap.common.config"})
@ConfigurationPropertiesScan(basePackages = {"io.shinhanlife.dap.biz.mcp.gateway", "io.shinhanlife.dap.common.mcp", "io.shinhanlife.dap.common.config"})
@EnableCaching
public class AxHubGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(AxHubGatewayApplication.class, args);
    }
}