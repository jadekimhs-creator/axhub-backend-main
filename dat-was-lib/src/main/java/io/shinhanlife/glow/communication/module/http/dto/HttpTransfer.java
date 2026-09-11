package io.shinhanlife.glow.communication.module.http.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/** Glow HTTP request envelope. Use HttpTransfer.http() to build a request. */
@Getter
@Builder(builderMethodName = "http")
public class HttpTransfer<T> {
    private final HttpHeader header;
    private final String domain;
    private final String uri;
    private final HttpMethod method;
    private final MediaType contentType;
    private final Class<?> responseEntity;
    private final T body;
}