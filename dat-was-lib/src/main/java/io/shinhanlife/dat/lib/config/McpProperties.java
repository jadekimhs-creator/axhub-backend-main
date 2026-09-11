package io.shinhanlife.dat.lib.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @package io.shinhanlife.dat.lib.config
 * @className McpProperties
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
@Data
@Configuration
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    private String namespace;
    private Manifest manifest = new Manifest();

    @Data
    public static class Manifest {
        private String bundleId;
        private String namePrefix;
    }

}