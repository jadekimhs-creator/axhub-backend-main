package io.shinhanlife.axhub.common.integration.mci.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @package io.shinhanlife.axhub.common.integration.mci.config
 * @className ShinhanIntegrationProperties
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
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "shinhan.integration")
public class ShinhanIntegrationProperties {
    
    /**
     * 환경유형코드: 운영(R), 테스트(T), 개발(D)
     */
    private String envrTypeCd = "D";

    private ServerInfo eai = new ServerInfo();
    private ServerInfo internalMci = new ServerInfo();

    @Getter
    @Setter
    public static class ServerInfo {
        private String url;
    }
}
