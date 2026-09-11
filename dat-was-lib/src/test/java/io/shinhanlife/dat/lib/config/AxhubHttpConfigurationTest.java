package io.shinhanlife.dat.lib.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.shinhanlife.glow.communication.module.http.component.GlowHttpComponent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

class AxhubHttpConfigurationTest {

    @Test
    void registersGlowHttpComponentFromDapLibConfiguration() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class)) {
            assertThat(context.getBean(GlowHttpComponent.class)).isNotNull();
        }
    }

    @Configuration
    @Import(AxhubHttpConfiguration.class)
    static class TestConfiguration {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }
}