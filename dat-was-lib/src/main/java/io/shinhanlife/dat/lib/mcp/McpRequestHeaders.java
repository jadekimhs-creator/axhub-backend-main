package io.shinhanlife.dat.lib.mcp;

/** Optional request headers propagated from an MCP HTTP request to a Tool invocation. */
public record McpRequestHeaders(
        String requestId,
        String guid,
        String mcpSessionId,
        String employeeNo,
        String virtualEmployeeNo) {
}
