package io.shinhanlife.dat.lib.mcp;

/** Holds optional MCP headers for the lifetime of one HTTP request thread. */
public final class McpRequestHeaderContext {
    private static final ThreadLocal<McpRequestHeaders> CURRENT_HEADERS = new ThreadLocal<>();

    private McpRequestHeaderContext() {
    }

    public static McpRequestHeaders current() {
        return CURRENT_HEADERS.get();
    }

    static void set(McpRequestHeaders headers) {
        CURRENT_HEADERS.set(headers);
    }

    static void clear() {
        CURRENT_HEADERS.remove();
    }
}