package io.shinhanlife.dat.lib.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Fallback properties for root-level tool-service-manifest.yml bindings
 * (e.g. `service_id: ...` and `routing_contract: ...`).
 */
@Data
@Configuration
@ConfigurationProperties
public class ToolServiceProperties {

    private String serviceId;
    private McpProperties.RoutingFunction routingContract;
    private List<McpProperties.RoutingFunction> routingFunctions = List.of();

    public boolean hasRoutingContract() {
        return routingContract != null;
    }

    public boolean hasServiceId() {
        return serviceId != null && !serviceId.isBlank();
    }
}
