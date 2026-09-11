package io.shinhanlife.dat.lib.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

/** Registers the Tool Pod's existing annotated tools with its MCP SDK server. */
@Component
@ConditionalOnBean(McpToolExecutionService.class)
public class ToolPodMcpToolSynchronizer {
    private final McpSyncServer mcpServer;
    private final ToolRegistryHeartbeatSender heartbeatSender;
    private final McpToolExecutionService toolExecutionService;
    private final ObjectMapper objectMapper;

    public ToolPodMcpToolSynchronizer(McpSyncServer mcpServer, ToolRegistryHeartbeatSender heartbeatSender,
                                      McpToolExecutionService toolExecutionService, ObjectMapper objectMapper) {
        this.mcpServer = mcpServer;
        this.heartbeatSender = heartbeatSender;
        this.toolExecutionService = toolExecutionService;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerLocalTools() {
        heartbeatSender.getAllScannedTools().stream()
                .filter(tool -> Boolean.TRUE.equals(tool.getVisible()))
                .forEach(tool -> mcpServer.addTool(specification(tool)));
    }

    private McpServerFeatures.SyncToolSpecification specification(ToolMetadata tool) {
        McpSchema.Tool mcpTool = ToolMetadataMcpMapper.toTool(tool);
        return McpServerFeatures.SyncToolSpecification.builder().tool(mcpTool)
                .callHandler((context, request) -> invoke(tool.getName(), McpRequestHeaderContext.current(), request.arguments())).build();
    }

    private McpSchema.CallToolResult invoke(String toolName, McpRequestHeaders requestHeaders,
                                            Map<String, Object> arguments) {
        ToolExecutionResult result = toolExecutionService.execute(toolName, requestHeaders, arguments);
        boolean failed = !result.isSuccess();
        Object body = result.body();
        try {
            return McpSchema.CallToolResult.builder().addTextContent(objectMapper.writeValueAsString(body))
                    .structuredContent(body).isError(failed).build();
        } catch (Exception error) {
            return McpSchema.CallToolResult.builder().addTextContent(String.valueOf(body)).isError(failed).build();
        }
    }

    private Map<String, Object> emptySchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of());
        schema.put("additionalProperties", false);
        return schema;
    }
    @SuppressWarnings("unchecked")
    private McpSchema.JsonSchema toJsonSchema(Map<String, Object> source) {
        Map<String, Object> schema = source == null ? emptySchema() : source;
        return new McpSchema.JsonSchema(
                String.valueOf(schema.getOrDefault("type", "object")),
                schema.get("properties") instanceof Map<?, ?> properties
                        ? (Map<String, Object>) properties : Map.of(),
                schema.get("required") instanceof List<?> required
                        ? (List<String>) required : List.of(),
                schema.get("additionalProperties") instanceof Boolean additionalProperties
                        ? additionalProperties : Boolean.TRUE,
                schema.get("$defs") instanceof Map<?, ?> defs ? (Map<String, Object>) defs : Map.of(),
                schema.get("definitions") instanceof Map<?, ?> definitions
                        ? (Map<String, Object>) definitions : Map.of());
    }
}
