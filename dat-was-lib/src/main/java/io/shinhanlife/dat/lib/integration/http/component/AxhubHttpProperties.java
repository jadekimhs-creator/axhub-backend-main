package io.shinhanlife.dat.lib.integration.http.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * Glow HTTP target catalog with hierarchical header support.
 *
 * <p>Supports global default headers and system-level headers (e.g., systems.eiam.headers).</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "glow.communication.http")
public class AxhubHttpProperties {

    private List<ApiDefinition> apiList = new ArrayList<>();
    private Map<String, SystemDefinition> systems = new HashMap<>();
    private Map<String, String> defaultHeaders = new HashMap<>();

    public record SystemDefinition(
            Map<String, String> headers
    ) {
    }

    public record ApiDefinition(
            String name,
            String domain,
            String url,
            HttpMethod method,
            String contentType,
            boolean bizPod
    ) {
    }
}