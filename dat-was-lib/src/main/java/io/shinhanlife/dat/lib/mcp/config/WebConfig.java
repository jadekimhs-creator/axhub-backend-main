package io.shinhanlife.dat.lib.mcp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import io.shinhanlife.dat.lib.mcp.filter.MdcLoggingFilter;
import io.shinhanlife.dat.lib.mcp.McpRequestHeaderFilter;
import io.shinhanlife.dat.lib.session.mci.AxhubSessionService;

import io.shinhanlife.dat.lib.mcp.security.ApiKeyInterceptor;

/**
 * @package io.shinhanlife.dat.lib.mcp.config
 * @className WebConfig
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 *
 * </pre>
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    // 1. 우리가 만든 인터셉터를 주입받습니다.
    private final ApiKeyInterceptor apiKeyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 2. 인터셉터 등록 및 검사할 URL 패턴 지정
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/rpc/**", "/mcp/**")
                .excludePathPatterns(
                        "/test/**", "/health", "/error", "/mcp/api/v1/admin/**",
                        "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**", // Swagger UI 경로는 인증 제외
                        "/mcp/api/v1/tools/docs/markdown", "/favicon.ico", "/mcp/api/v1/tools/list"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Swagger UI(8080)에서 Gateway(8081)로 API 호출 시 발생하는 CORS 에러 해결
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Mcp-Session-Id")
                .allowCredentials(true);
    }

    @Bean
    public FilterRegistrationBean<MdcLoggingFilter> mdcLoggingFilterRegistration() {
        FilterRegistrationBean<MdcLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MdcLoggingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setAsyncSupported(true);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<McpRequestHeaderFilter> mcpRequestHeaderFilterRegistration(
            @org.springframework.beans.factory.annotation.Autowired(required = false) AxhubSessionService sessionService) {
        FilterRegistrationBean<McpRequestHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new McpRequestHeaderFilter(sessionService));
        registration.addUrlPatterns("/*");
        registration.setOrder(2);
        registration.setAsyncSupported(true);
        return registration;
    }
}
