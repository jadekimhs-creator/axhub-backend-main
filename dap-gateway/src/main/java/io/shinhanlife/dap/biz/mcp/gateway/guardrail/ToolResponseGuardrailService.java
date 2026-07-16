package io.shinhanlife.dap.biz.mcp.gateway.guardrail;

import io.shinhanlife.dap.biz.mcp.gateway.resilience.FailureType;
import io.shinhanlife.dap.biz.mcp.gateway.resilience.ToolExecutionException;
import io.shinhanlife.dap.biz.mcp.gateway.dto.ToolMetadata;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tool 서버 응답을 Agent에게 전달하기 전에 환각 방어 규칙을 적용하는 서비스입니다.
 */
@Service
public class ToolResponseGuardrailService {
    private final ObjectMapper json;

    public ToolResponseGuardrailService(ObjectMapper json) {
        this.json = json;
    }

    /**
     * Tool 응답 data를 검증한 뒤 표준 MCP 응답 문자열로 감쌉니다.
     */
    public String validateAndWrap(ToolMetadata metadata, String requestId, JsonNode data) {
        validateData(metadata.getName(), data);

        ObjectNode response;
        if (data.isObject() && data.has("success") && data.has("data")) {
            response = ((ObjectNode) data).deepCopy();
            response.set("answerPolicy", answerPolicy());
        } else {
            response = json.createObjectNode();
            response.put("success", true);
            response.put("toolName", metadata.getName());
            response.put("requestId", requestId);
            response.put("source", "registered-tool-server");
            response.set("data", data);
            response.set("answerPolicy", answerPolicy());
        }

        try {
            return json.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (Exception error) {
            throw new ToolExecutionException(FailureType.INTERNAL_ERROR, "검증된 Tool 응답 직렬화에 실패했습니다.", error);
        }
    }

    /**
     * Tool 응답 data를 Agent에게 전달하기 전에 응답 규격으로 검증합니다.
     */
    public void validateData(String toolName, JsonNode data) {
        if (data == null || data.isMissingNode() || data.isNull()) {
            throw new ToolExecutionException(FailureType.HALLUCINATION_GUARDRAIL, "Tool 응답 data가 비어 있습니다. tool=" + toolName);
        }
        // 향후 ToolMetadata에 responseAllowedFields 등이 추가되면 스키마 검증 로직 추가
    }

    /**
     * Agent가 Tool 결과 밖의 내용을 추측하지 않도록 응답 정책을 함께 내려줍니다.
     */
    private ObjectNode answerPolicy() {
        ObjectNode policy = json.createObjectNode();
        policy.put("allowOnlyToolData", true);
        policy.put("noGuessing", true);
        policy.put("onMissingData", "answer_unknown_or_request_more_information");
        policy.put("instruction", "Tool이 반환한 data 필드만 사용하세요. data에 없는 값은 추측해서 만들지 마세요.");
        return policy;
    }
}
