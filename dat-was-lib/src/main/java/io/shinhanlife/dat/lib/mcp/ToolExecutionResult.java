package io.shinhanlife.dat.lib.mcp;

import java.util.Map;

/** Protocol-neutral result of invoking one Tool Pod business tool. */
public record ToolExecutionResult(int statusCode, Object body, Map<String, String> headers) {

    public ToolExecutionResult {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
}
