package io.shinhanlife.dat.lib.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import io.shinhanlife.dat.lib.util.ToolSchemaResolver;
import io.shinhanlife.dat.lib.validation.ToolArgumentSchemaValidator;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Executes a cached Tool independently from its HTTP or MCP transport. */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolExecutionService {

    private final McpToolMethodRegistry toolMethodRegistry;
    private final ObjectMapper objectMapper;
    private final ToolArgumentSchemaValidator toolArgumentSchemaValidator;
    private final ToolSchemaResolver toolSchemaResolver;

    public ToolExecutionResult execute(String functionName, McpRequestHeaders requestHeaders,
                                       Map<String, Object> arguments) {
        String requestId = requestHeaders == null ? null : requestHeaders.requestId();
        String guid = requestHeaders == null ? null : requestHeaders.guid();
        String mcpSessionId = requestHeaders == null ? null : requestHeaders.mcpSessionId();
        log.info("[Tool] IN - guid: {}, x-request-id: {}, tool: {}", guid, requestId, functionName);

        McpToolMethodRegistry.RegisteredTool resolvedTool = toolMethodRegistry.find(functionName);
        if (resolvedTool == null) {
            return error(404, "TOOL_NOT_FOUND", "Tool not found: " + functionName, requestId);
        }
        ToolExecutionResult validationFailure = validateInput(resolvedTool, arguments, requestId);
        if (validationFailure != null) {
            return validationFailure;
        }
        try {
            Object methodResult = invoke(resolvedTool, convertArgument(resolvedTool.method(), arguments));
            ToolExecutionResult outputFailure = validateOutput(resolvedTool, methodResult, requestId);
            if (outputFailure != null) {
                return outputFailure;
            }
            Map<String, String> headers = new HashMap<>();
            if (requestId != null) headers.put("x-request-id", requestId);
            if (guid != null) headers.put("guid", guid);
            if (mcpSessionId != null) headers.put("mcp-session-id", mcpSessionId);
            log.info("[Tool] OUT - guid: {}, x-request-id: {}, tool: {}", guid, requestId, functionName);
            return new ToolExecutionResult(200, methodResult, headers);
        } catch (Exception error) {
            log.error("[Tool] Tool execution failed. tool={}", functionName, error);
            return error(502, "TOOL_ERROR", "Tool execution failed", requestId);
        }
    }

    private ToolExecutionResult validateInput(McpToolMethodRegistry.RegisteredTool tool,
                                              Map<String, Object> arguments, String requestId) {
        if (tool.method().getParameterCount() == 0 || Map.class.isAssignableFrom(tool.method().getParameterTypes()[0])) return null;
        try {
            Map<String, Object> schema = toolSchemaResolver.resolve(tool.annotation(), tool.hint(), tool.method().getParameterTypes()[0]);
            ValidationResponse result = toolArgumentSchemaValidator.validate(schema, arguments);
            return result.valid() ? null : error(422, "INVALID_PARAM", "Tool arguments do not match the input schema", requestId);
        } catch (Exception error) {
            log.error("[Tool] Input schema validation failed unexpectedly. tool={}", tool.annotation().name(), error);
            return null;
        }
    }

    private Object convertArgument(Method method, Map<String, Object> arguments) {
        if (method.getParameterCount() == 0 || arguments == null || Map.class.isAssignableFrom(method.getParameterTypes()[0])) return arguments;
        return objectMapper.convertValue(arguments, method.getParameterTypes()[0]);
    }

    private Object invoke(McpToolMethodRegistry.RegisteredTool tool, Object argument) throws Exception {
        return tool.method().getParameterCount() == 0 ? tool.method().invoke(tool.bean()) : tool.method().invoke(tool.bean(), argument);
    }

    private ToolExecutionResult validateOutput(McpToolMethodRegistry.RegisteredTool tool,
                                               Object methodResult, String requestId) {
        try {
            Map<String, Object> outputSchema = toolSchemaResolver.resolveOutput(tool.annotation(), tool.method().getReturnType(), tool.hint());
            if (!outputSchema.isEmpty() && !toolArgumentSchemaValidator.validateValue(outputSchema, methodResult).valid()) {
                return error(500, "INVALID_TOOL_RESPONSE", "Tool response does not match its output schema", requestId);
            }
        } catch (Exception error) {
            log.error("[Tool] Output schema validation failed unexpectedly. tool={}", tool.annotation().name(), error);
        }
        return null;
    }

    private ToolExecutionResult error(int statusCode, String code, String message, String requestId) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("details", Map.of("status", Integer.toString(statusCode)));
        if (requestId != null) body.put("request_id", requestId);
        return new ToolExecutionResult(statusCode, body, Map.of());
    }
}
