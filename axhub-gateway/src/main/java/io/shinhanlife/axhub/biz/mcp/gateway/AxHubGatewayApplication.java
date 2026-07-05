package io.shinhanlife.axhub.biz.mcp.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AxHubGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(AxHubGatewayApplication.class, args);
    }
}
