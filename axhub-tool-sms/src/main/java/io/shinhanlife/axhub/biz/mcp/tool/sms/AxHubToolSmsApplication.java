package io.shinhanlife.axhub.biz.mcp.tool.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AxHubToolSmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AxHubToolSmsApplication.class, args);
    }
}
