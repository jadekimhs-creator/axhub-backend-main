package io.shinhanlife.axhub.biz.mcp.tool.other;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.other
 * @className AxHubToolOtherApplication
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
@SpringBootApplication(scanBasePackages = {"io.shinhanlife.axhub.biz.mcp.tool", "io.shinhanlife.axhub.biz.mcp.adapter", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"})
@org.springframework.boot.context.properties.ConfigurationPropertiesScan(basePackages = {"io.shinhanlife.axhub.biz.mcp.tool", "io.shinhanlife.axhub.biz.mcp.adapter", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"})
@EnableCaching
public class AxHubToolOtherApplication {
    public static void main(String[] args) {
        SpringApplication.run(AxHubToolOtherApplication.class, args);
    }
}