package io.shinhanlife.dat.lib.integration.http.component;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * Glow HTTP target catalog.
 *
 * <p>Each target follows the ShinhanLife standard: name, domain, url, method,
 * content-type, and biz-pod. Target-specific values belong in application-glow*.yml.</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "glow.communication.http")
public class AxhubHttpProperties {

    private List<ApiDefinition> apiList = new ArrayList<>();

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