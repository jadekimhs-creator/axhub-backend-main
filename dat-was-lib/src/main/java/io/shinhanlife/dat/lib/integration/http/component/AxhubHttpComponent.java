package io.shinhanlife.dat.lib.integration.http.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dat.lib.config.GlowCommunicationProperties;
import io.shinhanlife.dat.lib.mcp.McpRequestHeaderContext;
import io.shinhanlife.dat.lib.mcp.McpRequestHeaders;
import io.shinhanlife.glow.communication.module.http.component.GlowHttpComponent;
import io.shinhanlife.glow.communication.module.http.dto.HttpBody;
import io.shinhanlife.glow.communication.module.http.dto.HttpHeader;
import io.shinhanlife.glow.communication.module.http.dto.HttpTransfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Tool Pod outbound HTTP component using the Glow HTTP client.
 * Target URL, HTTP method, content type and Pod-to-Pod behaviour are resolved from
 * {@code glow.communication.http.api-list}; business source code does not own endpoint values.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AxhubHttpComponent {

    private static final String ANONYMOUS_REQUEST = "AXHUB-TOOL";

    private final GlowHttpComponent http;
    private final ObjectMapper json;
    private final GlowCommunicationProperties communicationProperties;
    private final AxhubHttpProperties properties;

    /** Calls the exact URL configured for the API name. */
    public <T, R> R call(String apiName, T inputDto, Class<R> responseBodyClass) {
        return call(apiName, "", inputDto, responseBodyClass, 0);
    }

    /** Calls the configured URL with an optional resource suffix. */
    public <T, R> R call(String apiName, String uri, T inputDto, Class<R> responseBodyClass) {
        return call(apiName, uri, inputDto, responseBodyClass, 0);
    }

    public <T, R> R call(String apiName, String uri, T inputDto, Class<R> responseBodyClass, int timeout) {
        AxhubHttpProperties.ApiDefinition api = resolveApi(apiName);
        return execute(api, uri, inputDto, responseBodyClass, timeout);
    }

    /** Calls the configured URL only when the target is marked as a business Pod. */
    public <T, R> R callBizPod(String apiName, T inputDto, Class<R> responseBodyClass) {
        AxhubHttpProperties.ApiDefinition api = resolveApi(apiName);
        if (!api.bizPod()) {
            throw new IllegalArgumentException("Configured API is not a business Pod: " + apiName);
        }
        return execute(api, "", inputDto, responseBodyClass, 0);
    }

    private <T, R> R execute(AxhubHttpProperties.ApiDefinition api, String uri, T inputDto,
                             Class<R> responseBodyClass, int timeout) {
        HttpHeader header = createHeader(api, timeout);
        String requestUri = joinPath(api.url(), uri);
        HttpTransfer<T> request = HttpTransfer.<T>http()
                .header(header)
                .domain(api.domain())
                .uri(requestUri)
                .method(api.method())
                .contentType(contentType(api))
                .responseEntity(responseBodyClass)
                .body(inputDto)
                .build();

        log.info("[AxhubHttpComponent] Glow HTTP call. apiName={}, method={}, uri={}",
                api.name(), api.method(), requestUri);
        ResponseEntity<HttpBody> response = http.sync(request);
        return convertResponse(response.getBody(), responseBodyClass);
    }

    private AxhubHttpProperties.ApiDefinition resolveApi(String apiName) {
        return properties.getApiList().stream()
                .filter(api -> apiName != null && apiName.equals(api.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No HTTP API configuration for name: " + apiName));
    }

    private HttpHeader createHeader(AxhubHttpProperties.ApiDefinition api, int timeout) {
        HttpHeader header = new HttpHeader();
        if (api.bizPod()) {
            header.set("X-POD-TO-POD", "true");
        }
        McpRequestHeaders inbound = McpRequestHeaderContext.current();
        if (inbound == null) {
            header.set("X-ANONYMOUS-REQ", ANONYMOUS_REQUEST);
        } else {
            putIfPresent(header, "x-request-id", inbound.requestId());
            putIfPresent(header, "guid", inbound.guid());
            putIfPresent(header, "mcp-session-id", inbound.mcpSessionId());
            putIfPresent(header, "employee-no", inbound.employeeNo());
            putIfPresent(header, "virtual-employee-no", inbound.virtualEmployeeNo());
        }
        header.setReadTimeout(timeout == 0 ? defaultReadTimeout() : timeout);
        return header;
    }

    private int defaultReadTimeout() {
        return communicationProperties == null || communicationProperties.getHttp() == null
                ? 0 : communicationProperties.getHttp().getReadTimeout();
    }

    private MediaType contentType(AxhubHttpProperties.ApiDefinition api) {
        return api.contentType() == null || api.contentType().isBlank()
                ? MediaType.APPLICATION_JSON
                : MediaType.parseMediaType(api.contentType());
    }

    private <R> R convertResponse(HttpBody responseBody, Class<R> responseBodyClass) {
        String content = responseBody == null ? null : responseBody.content();
        if (responseBodyClass == String.class) {
            return responseBodyClass.cast(content);
        }
        try {
            return json.readValue(content, responseBodyClass);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert HTTP response to " + responseBodyClass.getSimpleName(), e);
        }
    }

    private void putIfPresent(HttpHeader header, String name, String value) {
        if (value != null && !value.isBlank()) {
            header.set(name, value);
        }
    }

    private String joinPath(String configuredUrl, String suffix) {
        String left = configuredUrl == null ? "" : configuredUrl.replaceAll("/+$", "");
        if (suffix == null || suffix.isBlank()) {
            return left.isEmpty() ? "/" : left;
        }
        String right = suffix.startsWith("/") ? suffix : "/" + suffix;
        return left + right;
    }
}
