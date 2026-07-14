package io.shinhanlife.axhub.biz.mcp.gateway.sync;

import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.gateway.service.ExecuteService;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
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
    public McpStatelessServerFeatures.SyncToolSpecification create(ToolMetadata entry) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(entry.getName())
                .description(description(entry))
                .inputSchema(inputSchema(entry))
                .build();

        return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((context, request) -> execute(entry.getName(), request))
                .build();
    }

    private McpSchema.CallToolResult execute(String toolName, McpSchema.CallToolRequest request) {
        try {
            // Build legacy JSON-RPC payload format expected by ExecuteService
            Map<String, Object> payload = new HashMap<>();
            payload.put("jsonrpc", "2.0");
            payload.put("method", "tools/call");
            payload.put("id", UUID.randomUUID().toString());
            
            Map<String, Object> params = new HashMap<>();
            params.put("name", toolName);
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
        try {
            Map<String, Object> resultMap = objectMapper.convertValue(rawResult, new TypeReference<Map<String, Object>>() {});
            Object innerResult = resultMap.get("result");
            
            McpSchema.CallToolResult.Builder builder = McpSchema.CallToolResult.builder();
            builder.isError(resultMap.containsKey("error"));
            
            if (innerResult instanceof Map) {
                Map<String, Object> innerMap = (Map<String, Object>) innerResult;
                // If it follows structured content
                if (innerMap.containsKey("content")) {
                    List<Map<String, Object>> contentList = (List<Map<String, Object>>) innerMap.get("content");
                    for (Map<String, Object> item : contentList) {
                        if ("text".equals(item.get("type"))) {
                            builder.addTextContent((String) item.get("text"));
                        }
                    }
                } else {
                    // Fallback to text string representation
                    builder.addTextContent(objectMapper.writeValueAsString(innerResult));
                }
            } else {
                builder.addTextContent(String.valueOf(innerResult));
            }
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
