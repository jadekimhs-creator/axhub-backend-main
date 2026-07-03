package io.shinhanlife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Collections;

@SpringBootApplication
@ComponentScan(basePackages = {
    "io.shinhanlife.axhub.biz.mcp.gateway", 
    "io.shinhanlife.axhub.biz.mcp.adapter", 
    "io.shinhanlife.axhub.common.mcp"
})
public class AxHubGatewayApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AxHubGatewayApplication.class);
        // Gateway는 8081 포트에서 실행
        app.setDefaultProperties(Collections.singletonMap("server.port", "8081"));
        app.run(args);
    }
}
