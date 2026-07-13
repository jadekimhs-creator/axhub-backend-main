package io.shinhanlife.axhub.biz.mcp.tool.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {"io.shinhanlife.axhub.biz.mcp.tool", "io.shinhanlife.axhub.biz.mcp.adapter", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"})
@ConfigurationPropertiesScan(basePackages = {"io.shinhanlife.axhub.biz.mcp.tool", "io.shinhanlife.axhub.biz.mcp.adapter", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"})
@EnableCaching
public class PaymentToolApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentToolApplication.class, args);
    }
}
