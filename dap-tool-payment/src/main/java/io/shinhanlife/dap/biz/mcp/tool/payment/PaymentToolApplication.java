package io.shinhanlife.dap.biz.mcp.tool.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {"io.shinhanlife.dap.biz.mcp.tool", "io.shinhanlife.dap.biz.mcp.adapter", "io.shinhanlife.dap.common.mcp", "io.shinhanlife.dap.common.config"})
@ConfigurationPropertiesScan(basePackages = {"io.shinhanlife.dap.biz.mcp.tool", "io.shinhanlife.dap.biz.mcp.adapter", "io.shinhanlife.dap.common.mcp", "io.shinhanlife.dap.common.config"})
@EnableCaching
public class PaymentToolApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentToolApplication.class, args);
    }
}
