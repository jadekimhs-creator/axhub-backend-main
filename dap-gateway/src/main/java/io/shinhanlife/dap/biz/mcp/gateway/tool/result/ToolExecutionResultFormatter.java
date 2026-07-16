package io.shinhanlife.dap.biz.mcp.gateway.tool.result;

import io.shinhanlife.dap.biz.mcp.gateway.guardrail.SensitiveDataMasker;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Tool 서버 원시 응답을 MCP의 content 요약과 structuredContent로 분리합니다.
 */
@Service
public class ToolExecutionResultFormatter {
    private static final int TEXT_PREVIEW_LIMIT = 2_000;

    private final ObjectMapper json;
    private final SensitiveDataMasker masker;

    public ToolExecutionResultFormatter(ObjectMapper json, SensitiveDataMasker masker) {
        this.json = json;
        this.masker = masker;
    }

    /**
     * JSON 문자열을 content.text에 그대로 넣지 않고 structuredContent로 분리합니다.
     */
    public ToolExecutionResult fromRawResponse(String toolName, String rawResponse) {
        long sizeBytes = rawResponse == null ? 0 : rawResponse.getBytes(StandardCharsets.UTF_8).length;
        if (rawResponse == null || rawResponse.isBlank()) {
            return result(ToolExecutionResult.ResultType.EMPTY, "Tool 응답이 비어 있습니다.",
                    json.createObjectNode(), Map.of("toolName", toolName), false, false, sizeBytes);
        }
        try {
            JsonNode parsed = json.readTree(rawResponse);
            return fromJson(toolName, parsed, sizeBytes);
        } catch (Exception parseError) {
            ObjectNode metadata = json.createObjectNode();
            metadata.put("toolName", toolName);
            metadata.put("parseError", "JSON_PARSE_FAILED");
            metadata.put("preview", preview(rawResponse));
            return new ToolExecutionResult(
                    ToolExecutionResult.ResultType.PLAIN_TEXT,
                    List.of(new ToolExecutionResult.ContentItem("text", "Tool 응답이 JSON 형식이 아니어서 제한된 preview만 제공합니다.")),
                    null,
                    List.of(),
                    Map.of("toolName", toolName, "parseError", "JSON_PARSE_FAILED", "preview", preview(rawResponse)),
                    false,
                    rawResponse.length() > TEXT_PREVIEW_LIMIT,
                    sizeBytes);
        }
    }

    public ToolExecutionResult fromJson(String toolName, JsonNode parsed, long sizeBytes) {
        JsonNode masked = masker.mask(parsed);
        if (masked.isObject()) {
            ObjectNode object = (ObjectNode) masked;
            if (object.path("isError").asBoolean(false) || object.has("error") || object.has("failureType")) {
                return result(ToolExecutionResult.ResultType.ERROR, errorSummary(toolName, object), object,
                        metadata(toolName, sizeBytes), true, object.path("truncated").asBoolean(false), sizeBytes);
            }
            JsonNode structured = object.has("structuredContent") ? object.path("structuredContent") : structuredFromObject(object);
            return result(ToolExecutionResult.ResultType.JSON_OBJECT, summary(toolName, object, structured), structured,
                        metadata(toolName, sizeBytes), false, object.path("truncated").asBoolean(false), sizeBytes);
        }
        if (masked.isArray()) {
            ObjectNode structured = json.createObjectNode();
            structured.set("items", masked);
            structured.put("count", count(masked));
            return result(ToolExecutionResult.ResultType.JSON_ARRAY,
                    toolName + " 결과 " + count(masked) + "건을 조회했습니다.",
                    structured,
                    metadata(toolName, sizeBytes),
                    false,
                    false,
                    sizeBytes);
        }
        if (masked.isTextual()) {
            String text = masked.asText("");
            return result(ToolExecutionResult.ResultType.PLAIN_TEXT, preview(text), null,
                    metadata(toolName, sizeBytes), false, text.length() > TEXT_PREVIEW_LIMIT, sizeBytes);
        }
        ObjectNode structured = json.createObjectNode();
        structured.set("value", masked);
        return result(ToolExecutionResult.ResultType.JSON_OBJECT, toolName + " 결과를 조회했습니다.",
                structured, metadata(toolName, sizeBytes), false, false, sizeBytes);
    }

    private JsonNode structuredFromObject(ObjectNode object) {
        if (object.has("data")) {
            return object.path("data");
        }
        return object;
    }

    private String summary(String toolName, ObjectNode object, JsonNode structured) {
        String message = object.path("message").asText("");
        if (!message.isBlank()) {
            return message;
        }
        if (structured != null && structured.isObject()) {
            long totalCount = structured.path("totalCount").asLong(-1);
            if (totalCount >= 0) {
                return toolName + " 대량 결과 preview 조회가 완료되었습니다. totalCount=" + totalCount;
            }
        }
        if (structured != null && structured.isArray()) {
            return toolName + " 결과 " + count(structured) + "건을 조회했습니다.";
        }
        return toolName + " 조회가 완료되었습니다.";
    }

    private String errorSummary(String toolName, ObjectNode object) {
        String message = object.path("message").asText(object.path("error").asText(""));
        return message.isBlank() ? toolName + " 호출 중 오류가 발생했습니다." : message;
    }

    private ToolExecutionResult result(ToolExecutionResult.ResultType type, String text, JsonNode structuredContent,
                                       Map<String, Object> metadata, boolean error, boolean truncated, long sizeBytes) {
        return new ToolExecutionResult(
                type,
                List.of(new ToolExecutionResult.ContentItem("text", text)),
                structuredContent,
                List.of(),
                metadata,
                error,
                truncated,
                sizeBytes);
    }

    private Map<String, Object> metadata(String toolName, long sizeBytes) {
        return Map.of("toolName", toolName, "originalSizeBytes", sizeBytes);
    }

    private int count(JsonNode node) {
        int count = 0;
        for (JsonNode ignored : node) {
            count++;
        }
        return count;
    }

    private String preview(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= TEXT_PREVIEW_LIMIT ? value : value.substring(0, TEXT_PREVIEW_LIMIT);
    }
}
