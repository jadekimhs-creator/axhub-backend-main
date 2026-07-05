package io.shinhanlife.axhub.biz.mcp.tool.other;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AxHubToolOtherApplication {
    public static void main(String[] args) {
        SpringApplication.run(AxHubToolOtherApplication.class, args);
    }
}
