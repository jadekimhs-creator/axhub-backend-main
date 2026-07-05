package io.shinhanlife.axhub.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 모든 엔드포인트에 대해 CORS 허용
                .allowedOrigins("http://localhost:3006")  // Vue 앱의 오리진 허용 (필요 시 여러 오리진 추가: .allowedOrigins("http://localhost:3006", "https://example.com"))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 허용할 HTTP 메서드
                .allowedHeaders("*")  // 모든 헤더 허용
                .allowCredentials(true)  // 쿠키/인증 정보 허용 (필요 시)
                .maxAge(3600);  // preflight 요청 캐시 시간 (초 단위)
    }
}