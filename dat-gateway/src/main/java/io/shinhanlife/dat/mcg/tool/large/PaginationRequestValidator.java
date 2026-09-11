package io.shinhanlife.dat.mcg.tool.large;


/**
 * @package io.shinhanlife.dat.mcg.tool.large
 * @className PaginationRequestValidator
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 * 
 * </pre>
 */
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import io.shinhanlife.dat.mcg.config.McpGatewayProperties;
import io.shinhanlife.dat.mcg.resilience.FailureType;
import io.shinhanlife.dat.mcg.resilience.ToolExecutionException;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tool 서버로 전달할 pagination 요청값을 MCP 정책 기준으로 검증하고 보정합니다.
 */
@Component
public class PaginationRequestValidator {
    private static final int MAX_CURSOR_LENGTH = 2048;

    private final McpGatewayProperties properties;
    private final ObjectMapper json;

    public PaginationRequestValidator(McpGatewayProperties properties, ObjectMapper json) {
        this.properties = properties;
        this.json = json;
    }

    /**
     * pageSize/cursor를 검증한 뒤 Tool 서버에 넘길 안전한 arguments 복사본을 만듭니다.
     */
    public ObjectNode normalize(ToolMetadata metadata, ObjectNode arguments) {
        try {
            ObjectNode normalized = arguments == null
                    ? json.createObjectNode()
                    : (ObjectNode) json.readTree(json.writeValueAsString(arguments));
            
            if (metadata != null && metadata.allowedArguments().contains("pageSize")) {
                normalizePageSize(normalized);
            } else if (normalized.has("pageSize")) {
                normalizePageSize(normalized);
            }
            
            validateCursor(normalized);
            return normalized;
        } catch (ToolExecutionException error) {
            throw error;
        } catch (Exception error) {
            throw standardError("INVALID_PAGINATION_REQUEST",
                    "Pagination 요청값을 검증하는 중 오류가 발생했습니다.",
                    detail("cause", error.getMessage()));
        }
    }

    private void normalizePageSize(ObjectNode normalized) {
        int pageSize = normalized.has("pageSize")
                ? normalized.path("pageSize").asInt(-1)
                : properties.defaultPageSize();
        if (pageSize < 1) {
            throw standardError("INVALID_PAGINATION_REQUEST",
                    "pageSize는 1 이상이어야 합니다.",
                    detail("requestedPageSize", pageSize));
        }
        if (pageSize > properties.maxPageSize()) {
            ObjectNode detail = detail("requestedPageSize", pageSize);
            detail.put("maxPageSize", properties.maxPageSize());
            detail.put("suggestedPageSize", properties.defaultPageSize());
            throw standardError("PAGE_SIZE_EXCEEDED",
                    "요청한 pageSize가 MCP 최대 허용값을 초과했습니다.",
                    detail);
        }
        normalized.put("pageSize", pageSize);
    }

    private void validateCursor(ObjectNode normalized) {
        if (!normalized.has("cursor")) {
            return;
        }
        String cursor = normalized.path("cursor").asText("");
        if (cursor.length() > MAX_CURSOR_LENGTH) {
            ObjectNode detail = detail("cursorLength", cursor.length());
            detail.put("maxCursorLength", MAX_CURSOR_LENGTH);
            throw standardError("INVALID_CURSOR",
                    "cursor 값이 너무 깁니다.",
                    detail);
        }
    }

    private ToolExecutionException standardError(String code, String message, ObjectNode detail) {
        ObjectNode error = json.createObjectNode();
        error.put("isError", true);
        error.put("errorCode", code);
        error.put("message", message);
        error.set("details", detail);
        try {
            return new ToolExecutionException(FailureType.CLIENT_ERROR, json.writeValueAsString(error));
        } catch (Exception serializeError) {
            return new ToolExecutionException(FailureType.CLIENT_ERROR, code + ": " + message);
        }
    }

    private ObjectNode detail(String name, Object value) {
        ObjectNode detail = json.createObjectNode();
        if (value instanceof Number number) {
            detail.put(name, number.longValue());
        } else {
            detail.put(name, value == null ? "" : String.valueOf(value));
        }
        return detail;
    }
}
