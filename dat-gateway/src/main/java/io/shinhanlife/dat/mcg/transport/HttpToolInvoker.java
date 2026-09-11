package io.shinhanlife.dat.mcg.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.shinhanlife.dat.mcg.resilience.FailureType;
import io.shinhanlife.dat.mcg.resilience.ToolExecutionException;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** MCP SDK client used for Gateway-to-Tool-Pod calls over Streamable HTTP. */
@Slf4j
@Component
public class HttpToolInvoker implements ToolInvoker {

    private final ObjectMapper objectMapper;

    public HttpToolInvoker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode invoke(String toolName, Map<String, Object> arguments, String podUrl, Map<String, String> headers) {
        String endpoint = podUrl.replaceAll("/+$", "") + "/mcp";
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(endpoint)
                .requestBuilder(requestBuilder)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        try (McpSyncClient client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("dat-gateway", "1.0.0"))
                .requestTimeout(Duration.ofSeconds(30))
                .build()) {
            client.initialize();
            McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder()
                    .name(toolName)
                    .arguments(arguments)
                    .build());
            if (Boolean.TRUE.equals(result.isError())) {
                throw new ToolExecutionException(FailureType.BUSINESS_ERROR, "Tool Pod MCP error: " + textContent(result));
            }
            return result.structuredContent() != null
                    ? objectMapper.valueToTree(result.structuredContent())
                    : textContentAsJson(result);
        } catch (ToolExecutionException error) {
            throw error;
        } catch (Exception error) {
            throw new ToolExecutionException(FailureType.SERVER_ERROR,
                    "Tool Pod MCP call failed: " + error.getMessage(), error);
        }
    }

    private JsonNode textContentAsJson(McpSchema.CallToolResult result) {
        String text = textContent(result);
        try {
            return objectMapper.readTree(text);
        } catch (Exception ignored) {
            return TextNode.valueOf(text);
        }
    }

    private String textContent(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .findFirst()
                .orElse("");
    }
}
