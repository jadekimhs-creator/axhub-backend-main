package io.shinhanlife.dat.lib.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import io.shinhanlife.dat.lib.util.ToolSchemaResolver;
import io.shinhanlife.dat.lib.validation.ToolArgumentSchemaValidator;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
            Map<String, Object> rawSchema = toolSchemaResolver.resolve(tool.annotation(), tool.hint(), tool.method().getParameterTypes()[0]);
            Map<String, Object> normalizedSchema = ToolMetadataMcpMapper.normalizeSchema(rawSchema);

            ValidationResponse result = toolArgumentSchemaValidator.validate(normalizedSchema, arguments);
            if (!result.valid() && !rawSchema.equals(normalizedSchema)) {
                result = toolArgumentSchemaValidator.validate(rawSchema, arguments);
            }
            return result.valid() ? null : error(422, "INVALID_PARAM", "Tool arguments do not match the input schema", requestId);
        } catch (Exception error) {
            log.error("[Tool] Input schema validation failed unexpectedly. tool={}", tool.annotation().name(), error);
            return null;
        }
    }

    private Object convertArgument(Method method, Map<String, Object> arguments) {
        if (method.getParameterCount() == 0 || arguments == null || Map.class.isAssignableFrom(method.getParameterTypes()[0])) return arguments;
        Map<String, Object> adapted = adaptArguments(method.getParameterTypes()[0], arguments);
        return objectMapper.convertValue(adapted, method.getParameterTypes()[0]);
    }

    /**
     * Agent Builder / MCP 표준에 맞춰 최상위 평탄화(Flat)되어 전달된 인자를
     * 대상 DTO의 단일 래퍼(단일 Object 또는 단일 List) 구조에 맞게 자동으로 감싸줍니다.
     */
    static Map<String, Object> adaptArguments(Class<?> targetType, Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty() || targetType == null) {
            return arguments;
        }
        Field[] declaredFields = getDeclaredBusinessFields(targetType);
        if (declaredFields.length == 1) {
            Field singleField = declaredFields[0];
            String fieldName = singleField.getName();
            // 클라이언트가 이미 해당 래퍼 필드명으로 감싸서 전달한 경우에는 추가 래핑하지 않음
            if (!arguments.containsKey(fieldName)) {
                Class<?> fieldType = singleField.getType();
                if (List.class.isAssignableFrom(fieldType)) {
                    // 단일 List 래퍼인 경우 -> [{...}] 형태로 자동 포장
                    Map<String, Object> adapted = new LinkedHashMap<>();
                    adapted.put(fieldName, List.of(new LinkedHashMap<>(arguments)));
                    return adapted;
                } else if (!isSimpleType(fieldType)) {
                    // 단일 Object DTO 래퍼인 경우 -> {...} 형태로 자동 포장
                    Map<String, Object> adapted = new LinkedHashMap<>();
                    adapted.put(fieldName, new LinkedHashMap<>(arguments));
                    return adapted;
                }
            }
        }
        return arguments;
    }

    private static Field[] getDeclaredBusinessFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
                fields.add(f);
            }
        }
        return fields.toArray(new Field[0]);
    }

    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == String.class
                || Number.class.isAssignableFrom(clazz)
                || clazz == Boolean.class
                || clazz == Character.class
                || Map.class.isAssignableFrom(clazz)
                || clazz == Object.class;
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
