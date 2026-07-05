package io.shinhanlife.axhub.biz.mcp.tool.email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AxHubToolEmailApplication {
    public static void main(String[] args) {
        SpringApplication.run(AxHubToolEmailApplication.class, args);
    }
}
