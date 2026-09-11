package io.shinhanlife.dat.lib.integration.mci.config;

import io.shinhanlife.glow.communication.module.mci.component.GlowMciComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TODO: 실제 Glow Framework 의존성이 추가되어 io.shinhanlife.glow 패키지가 
 * ComponentScan에 잡히게 되면 이 설정 클래스는 삭제하세요.
 */
@Configuration

/**
 * @package io.shinhanlife.dat.lib.integration.mci.config
 * @className GlowMockConfig
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
public class GlowMockConfig {

    @Bean
    @SuppressWarnings("rawtypes")
    public GlowMciComponent glowMciComponent() {
        return new GlowMciComponent();
    }

    @Bean
    @SuppressWarnings("rawtypes")
    public io.shinhanlife.glow.communication.module.eai.component.GlowEaiComponent glowEaiComponent() {
        return new io.shinhanlife.glow.communication.module.eai.component.GlowEaiComponent();
    }
}
