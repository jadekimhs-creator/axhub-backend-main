package io.shinhanlife.dap.biz.mcp.gateway;


/**
 * @package io.shinhanlife.dap.biz.mcp.gateway
 * @className DapGatewayApplication
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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {"io.shinhanlife.dap.biz.mcp.gateway", "io.shinhanlife.dap.common.mcp", "io.shinhanlife.dap.common.config"})
@ConfigurationPropertiesScan(basePackages = {"io.shinhanlife.dap.biz.mcp.gateway", "io.shinhanlife.dap.common.mcp", "io.shinhanlife.dap.common.config"})
@EnableCaching
public class DapGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(DapGatewayApplication.class, args);
    }
}
