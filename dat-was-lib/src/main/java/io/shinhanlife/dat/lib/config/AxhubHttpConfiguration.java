package io.shinhanlife.dat.lib.config;

import io.shinhanlife.glow.communication.module.http.component.GlowHttpComponent;
import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Registers the temporary Glow HTTP compatibility component from the DAP library scan scope.
 * The bean is only created when an official GlowHttpComponent has not already been supplied.
 */
@Configuration(proxyBeanMethods = false)
public class AxhubHttpConfiguration {

    @Bean
    @ConditionalOnMissingBean(GlowHttpComponent.class)
    public GlowHttpComponent glowHttpComponent(RestClient.Builder restClientBuilder) {
        // WireMock and legacy internal endpoints can only support HTTP/1.1.
        // Avoid JDK HTTP/2 negotiation that may cause RST_STREAM responses.
        HttpClient http11Client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(http11Client);
        return new GlowHttpComponent(restClientBuilder.requestFactory(requestFactory));
    }
}