package io.shinhanlife.dat.mcg;


/**
 * @package io.shinhanlife.dat.mcg
 * @className DatGatewayApplication
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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"io.shinhanlife.dat.mcg", "io.shinhanlife.dat.lib.mcp", "io.shinhanlife.dat.lib.config"})
@ConfigurationPropertiesScan(basePackages = {"io.shinhanlife.dat.mcg", "io.shinhanlife.dat.lib.mcp", "io.shinhanlife.dat.lib.config"})
@EnableCaching
@EnableScheduling
public class DatGatewayApplication {
    public static void main(String[] args) {
        new org.springframework.boot.builder.SpringApplicationBuilder(DatGatewayApplication.class).headless(false).run(args);
    }
}
