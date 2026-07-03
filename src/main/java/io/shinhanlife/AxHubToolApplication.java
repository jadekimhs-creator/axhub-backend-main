package io.shinhanlife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
    "io.shinhanlife.axhub.biz.mcp.tool", 
    "io.shinhanlife.axhub.biz.mcp.adapter", 
    "io.shinhanlife.axhub.common.mcp"
})
public class AxHubToolApplication {
    public static void main(String[] args) {
        // 기본 포트는 8082로 설정하되, 파라미터(--server.port=808x)가 있으면 해당 포트로 설정
        String port = "8082";
        for (String arg : args) {
            if (arg.startsWith("--server.port=")) {
                port = arg.substring("--server.port=".length());
            }
        }
        System.setProperty("server.port", port);
        
        SpringApplication app = new SpringApplication(AxHubToolApplication.class);
        app.run(args);
    }
}
