package io.shinhanlife.dap.mcc.component;

import io.shinhanlife.glow.communication.module.mci.component.GlowMciComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TODO: 실제 Glow Framework 의존성이 추가되어 io.shinhanlife.glow 패키지가 
 * ComponentScan에 잡히게 되면 이 설정 클래스는 삭제하세요.
 */
@Configuration
public class GlowMockConfig {

    @Bean
    @SuppressWarnings("rawtypes")
    public GlowMciComponent glowMciComponent() {
        return new GlowMciComponent();
    }
}
