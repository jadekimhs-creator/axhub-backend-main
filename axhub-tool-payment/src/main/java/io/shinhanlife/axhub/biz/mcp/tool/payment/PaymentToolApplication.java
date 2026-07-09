package io.shinhanlife.axhub.biz.mcp.tool.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication(scanBasePackages = "io.shinhanlife.axhub.biz.mcp")
public class PaymentToolApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentToolApplication.class, args);
    }
}
