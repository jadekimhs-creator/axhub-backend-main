package io.shinhanlife.dap.biz.mcp.gateway.tool.result;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool 실행 결과를 text 요약과 structuredContent로 분리해서 표현합니다.
 */
public record ToolExecutionResult(
        ResultType resultType,
        List<ContentItem> content,
        JsonNode structuredContent,
        List<ResourceLink> resourceLinks,
        Map<String, Object> metadata,
        boolean isError,
        boolean truncated,
        long originalSizeBytes
) {
    public record ContentItem(String type, String text) {
    }

    public record ResourceLink(String uri, String name, String mimeType, long sizeBytes) {
    }

    public enum ResultType {
        JSON_OBJECT,
        JSON_ARRAY,
        PLAIN_TEXT,
        BINARY_FILE,
        EMPTY,
        ERROR
    }
}
