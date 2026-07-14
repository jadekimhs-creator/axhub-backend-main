package io.shinhanlife.axhub.biz.mcp.gateway.sync;

import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.gateway.service.ExecuteService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.shinhanlife.axhub.biz.mcp.gateway.tool.result.ToolExecutionResult;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registry에 등록된 실제 Tool 메타데이터를 MCP 표준 Tool specification으로 변환합니다.
 */
@Component
public class RegistryMcpToolSpecificationFactory {
    private final ExecuteService executeService;
    private final ObjectMapper objectMapper;

    public RegistryMcpToolSpecificationFactory(ExecuteService executeService, ObjectMapper objectMapper) {
        this.executeService = executeService;
        this.objectMapper = objectMapper;
    }

    /**
     * Registry Entry 하나를 MCP SDK의 stateless sync Tool specification으로 변환합니다.
     */
    public McpServerFeatures.SyncToolSpecification create(ToolMetadata entry) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(entry.getName())
                .description(description(entry))
                .inputSchema(inputSchema(entry))
                .build();

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((context, request) -> execute(entry, request))
                .build();
    }

    private McpSchema.CallToolResult execute(ToolMetadata entry, McpSchema.CallToolRequest request) {
        try {
            // Build legacy JSON-RPC payload format expected by ExecuteService
            Map<String, Object> payload = new HashMap<>();
            payload.put("jsonrpc", "2.0");
            payload.put("method", "tools/call");
            payload.put("id", UUID.randomUUID().toString());
            
            Map<String, Object> params = new HashMap<>();
            params.put("name", entry.getName());
            params.put("arguments", request.arguments());
            payload.put("params", params);

            // Execute through ExecuteService
            Object rawResult = executeService.execute(payload, "system");
            
            // Convert JSON-RPC response back to McpSchema.CallToolResult
            return toCallToolResult(rawResult);
        } catch (Exception error) {
            return McpSchema.CallToolResult.builder()
                    .addTextContent("Tool 실행 중 내부 오류가 발생했습니다: " + error.getMessage())
                    .isError(true)
                    .build();
        }
    }

    private McpSchema.CallToolResult toCallToolResult(Object rawResult) {
        if (rawResult instanceof ToolExecutionResult result) {
            McpSchema.CallToolResult.Builder builder = McpSchema.CallToolResult.builder()
                    .isError(result.isError())
                    .meta(result.metadata());

            if (result.content() == null || result.content().isEmpty()) {
                builder.addTextContent("");
            } else {
                for (ToolExecutionResult.ContentItem item : result.content()) {
                    if ("text".equals(item.type())) {
                        builder.addTextContent(item.text());
                    }
                }
            }

            if (result.structuredContent() != null) {
                builder.structuredContent(result.structuredContent());
            }
            return builder.build();
        }

        // Fallback for old map format or unexpected types
        try {
            Map<String, Object> resultMap = objectMapper.convertValue(rawResult, new TypeReference<Map<String, Object>>() {});
            if (resultMap.containsKey("resultType") || resultMap.containsKey("structuredContent")) {
                ToolExecutionResult result = objectMapper.convertValue(rawResult, ToolExecutionResult.class);
                return toCallToolResult(result);
            }
            
            Object innerResult = resultMap.containsKey("result") ? resultMap.get("result") : resultMap;
            
            McpSchema.CallToolResult.Builder builder = McpSchema.CallToolResult.builder();
            builder.isError(resultMap.containsKey("error"));
            builder.addTextContent(objectMapper.writeValueAsString(innerResult));
            return builder.build();
        } catch (Exception e) {
            return McpSchema.CallToolResult.builder()
                    .addTextContent(String.valueOf(rawResult))
                    .build();
        }
    }

    private Map<String, Object> inputSchema(ToolMetadata entry) {
        if (entry.getParametersSchema() != null) {
            return entry.getParametersSchema();
        }
        
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", new HashMap<>());
        schema.put("additionalProperties", false);
        return schema;
    }

    private String description(ToolMetadata entry) {
        return entry.getDescription() == null || entry.getDescription().isBlank()
                ? entry.getName() + " Tool"
                : entry.getDescription();
    }
}
