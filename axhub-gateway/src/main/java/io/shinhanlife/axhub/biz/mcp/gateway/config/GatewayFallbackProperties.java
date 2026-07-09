package io.shinhanlife.axhub.biz.mcp.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.gateway.config
 * @className GatewayFallbackProperties
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mcp.gateway.fallback")
public class GatewayFallbackProperties {
    private Map<String, String> routes = new HashMap<>();
    private String defaultUrl;
}