package io.shinhanlife.dap.biz.mcp.gateway.transport;

import io.shinhanlife.dap.biz.mcp.gateway.resilience.FailureType;
import io.shinhanlife.dap.biz.mcp.gateway.resilience.ToolExecutionException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class HttpToolInvoker implements ToolInvoker {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpToolInvoker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    @Override
    public JsonNode invoke(Map<String, Object> payload, String targetUrl) {
        try {
            Object httpResult = restClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    // TODO: Use actual tenant's key
                    .header("X-Trace-Id", UUID.randomUUID().toString())
                    .body(payload)
                    .retrieve()
                    .body(Object.class);
            
            return extractData(objectMapper.valueToTree(httpResult));
        } catch (Exception e) {
            throw new ToolExecutionException(FailureType.SERVER_ERROR, "Tool Pod HTTP 호출 실패: " + e.getMessage(), e);
        }
    }

    private JsonNode extractData(JsonNode root) {
        if (!root.path("success").asBoolean(true)) {
            throw new ToolExecutionException(FailureType.BUSINESS_ERROR, "Tool 서버 업무 오류: " + root.path("error").asText());
        }
        return root.has("data") ? root.get("data") : root;
    }
}
