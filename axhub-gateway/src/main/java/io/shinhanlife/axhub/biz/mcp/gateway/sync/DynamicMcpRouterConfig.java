package io.shinhanlife.axhub.biz.mcp.gateway.sync;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @package io.shinhanlife.axhub.biz.mcp.gateway.sync
 * @className DynamicMcpRouterConfig
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
@Configuration
public class DynamicMcpRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> dynamicMcpRouterFunction(DynamicMcpServerManager manager) {
        return manager.getDynamicRouter();
    }
}
