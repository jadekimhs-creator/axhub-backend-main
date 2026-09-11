package io.shinhanlife.glow.communication.module.http.component;

import io.shinhanlife.glow.communication.ICommunication;
import io.shinhanlife.glow.communication.module.http.dto.HttpBody;
import io.shinhanlife.glow.communication.module.http.dto.HttpHeader;
import io.shinhanlife.glow.communication.module.http.dto.HttpTransfer;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Temporary compatibility implementation of the internal Glow HTTP component.
 * Replace this class with the official Glow HTTP JAR when it is supplied.
 */
@Component
public class GlowHttpComponent implements ICommunication<HttpTransfer<?>, ResponseEntity<HttpBody>> {

    private final RestClient restClient;

    public GlowHttpComponent(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public ResponseEntity<HttpBody> sync(HttpTransfer<?> request) {
        if (request == null || request.getHeader() == null || request.getMethod() == null
                || !StringUtils.hasText(request.getDomain())) {
            throw new IllegalArgumentException("Glow HTTP request header, domain, and method are required.");
        }
        if (HttpMethod.GET.equals(request.getMethod())) {
            return get(request);
        }
        if (HttpMethod.POST.equals(request.getMethod())) {
            return post(request);
        }
        if (HttpMethod.PUT.equals(request.getMethod())) {
            return put(request);
        }
        if (HttpMethod.DELETE.equals(request.getMethod())) {
            return delete(request);
        }
        throw new IllegalArgumentException("Unsupported HTTP method: " + request.getMethod());
    }

    private ResponseEntity<HttpBody> get(HttpTransfer<?> request) {
        return toHttpBody(restClient.get().uri(buildUri(request, true))
                .headers(headers -> applyHeaders(headers, request.getHeader()))
                .retrieve().toEntity(String.class));
    }

    private ResponseEntity<HttpBody> post(HttpTransfer<?> request) {
        return toHttpBody(restClient.post().uri(buildUri(request, false))
                .contentType(contentType(request)).headers(headers -> applyHeaders(headers, request.getHeader()))
                .body(request.getBody()).retrieve().toEntity(String.class));
    }

    private ResponseEntity<HttpBody> put(HttpTransfer<?> request) {
        return toHttpBody(restClient.put().uri(buildUri(request, false))
                .contentType(contentType(request)).headers(headers -> applyHeaders(headers, request.getHeader()))
                .body(request.getBody()).retrieve().toEntity(String.class));
    }

    private ResponseEntity<HttpBody> delete(HttpTransfer<?> request) {
        return toHttpBody(restClient.delete().uri(buildUri(request, true))
                .headers(headers -> applyHeaders(headers, request.getHeader()))
                .retrieve().toEntity(String.class));
    }

    private String buildUri(HttpTransfer<?> request, boolean includeQueryParameters) {
        String uri = request.getDomain() + (request.getUri() == null ? "" : request.getUri());
        if (!includeQueryParameters || !(request.getBody() instanceof Map<?, ?> parameters)) {
            return uri;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uri);
        parameters.forEach((key, value) -> { if (key != null && value != null) builder.queryParam(String.valueOf(key), value); });
        return builder.build().encode().toUriString();
    }

    private MediaType contentType(HttpTransfer<?> request) {
        return request.getContentType() == null ? MediaType.APPLICATION_JSON : request.getContentType();
    }

    private void applyHeaders(HttpHeaders target, HttpHeader source) {
        target.setAccept(List.of(MediaType.APPLICATION_JSON));
        source.getValues().forEach((name, value) -> { if (StringUtils.hasText(name) && StringUtils.hasText(value)) target.set(name, value); });
    }

    private ResponseEntity<HttpBody> toHttpBody(ResponseEntity<String> response) {
        return new ResponseEntity<>(new HttpBody(response.getBody()), response.getHeaders(), response.getStatusCode());
    }
}