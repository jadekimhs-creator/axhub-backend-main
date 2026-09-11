package io.shinhanlife.glow.communication.module.http.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** Glow HTTP request headers and timeout metadata. */
@Getter
@Setter
public class HttpHeader {
    private Map<String, String> values = new LinkedHashMap<>();
    private int readTimeout;

    public void set(String name, String value) {
        values.put(name, value);
    }
}