package io.shinhanlife.axhub.common.mcp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shinhan MCP Gateway API 명세서")
                        .version("v1.0")
                        .description("AI Agent와 신한라이프 내부망(EIMS/EAI)을 연결하는 Adapter Gateway API 문서입니다."))
                .addServersItem(new io.swagger.v3.oas.models.servers.Server().url("http://localhost:8080").description("Adapter Pod (8080)"))
                .addServersItem(new io.swagger.v3.oas.models.servers.Server().url("http://localhost:8081").description("Gateway Pod (8081)"))
                // 전역적으로 X-API-KEY 보안 설정을 Swagger UI에 추가합니다.
                .addSecurityItem(new SecurityRequirement().addList("X-API-KEY"))
                .components(new Components()
                        .addSecuritySchemes("X-API-KEY",
                                new SecurityScheme()
                                        .name("X-API-KEY")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("헤더에 API Key를 입력해주세요. (기본값: SHINHAN_MCP_SECRET_KEY_2026)")));
    }
}
