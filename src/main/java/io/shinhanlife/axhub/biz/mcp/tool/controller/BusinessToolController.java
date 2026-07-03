package io.shinhanlife.axhub.biz.mcp.tool.controller;

import io.shinhanlife.axhub.biz.mcp.tool.model.ToolConfig;
import io.shinhanlife.axhub.biz.mcp.tool.model.ToolFunction;
import io.shinhanlife.axhub.biz.mcp.tool.registry.MockToolAutoRegistrar;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import org.springframework.context.ApplicationContext;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/tool")
@RequiredArgsConstructor
public class BusinessToolController {

    private final ApplicationContext applicationContext;
    private final MockToolAutoRegistrar toolRegistrar;
    private final ObjectMapper objectMapper;

    //  단일 동적 라우팅 엔드포인트
    @PostMapping("/execute/{toolName}")
    public Map<String, Object> executeDynamicTool(@PathVariable String toolName, @RequestBody(required = false) Map<String, Object> payload) {
        log.info("\n [Tool] 동적 툴 실행 요청 수신: {}", toolName);
        if (payload != null) {
            try {
                log.info(" [Tool] 호출 파라미터: {}", objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                log.info(" [Tool] 호출 파라미터: {}", payload);
            }
        }

        // 1. 등록된 툴 설정 조회
        ToolConfig config = toolRegistrar.getTools().stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst()
                .orElse(null);

        if (config == null) {
            log.error(" [Tool] 등록되지 않은 툴 호출: {}", toolName);
            Map<String, Object> error = new HashMap<>();
            error.put("jsonrpc", "2.0");
            error.put("error", Map.of("code", -32601, "message", "등록되지 않은 툴입니다: " + toolName));
            error.put("id", payload != null ? payload.get("id") : null);
            return error;
        }

        // 2. 파라미터에서 실행할 함수(action) 추출 및 라우팅 결정 (JSON-RPC)
        Map<String, Object> params = payload != null ? (Map<String, Object>) payload.get("params") : null;
        Map<String, Object> arguments = params != null ? (Map<String, Object>) params.get("arguments") : null;
        String action = arguments != null && arguments.containsKey("action") ? arguments.get("action").toString() : null;
        
        ToolFunction targetFunction = null;
        if (action != null) {
            targetFunction = config.getFunctions().stream()
                    .filter(f -> f.getName().equals(action))
                    .findFirst()
                    .orElse(null);
        }
        
        // action 파라미터가 없거나 일치하는 함수가 없으면 첫 번째 함수를 기본값으로 사용
        if (targetFunction == null && !config.getFunctions().isEmpty()) {
            targetFunction = config.getFunctions().get(0);
            log.warn(" [Tool] 지정된 action 파라미터가 없거나 유효하지 않아 기본 함수({})를 사용합니다.", targetFunction.getName());
        }

        String interfaceId = targetFunction != null ? targetFunction.getInterfaceId() : "UNKNOWN";

        // 2-1. 파라미터 유효성 검증 (JSON Schema)
        if (targetFunction != null && targetFunction.getParameterSchema() != null && !targetFunction.getParameterSchema().equals("{}")) {
            try {
                // parameterSchema에는 properties 내용만 들어있으므로 완전한 스키마 형태로 감싸줍니다.
                String fullSchemaJson = "{\"type\":\"object\", \"properties\":" + targetFunction.getParameterSchema() + "}";
                
                JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
                JsonSchema schema = factory.getSchema(fullSchemaJson);
                
                Set<ValidationMessage> errors = schema.validate(objectMapper.valueToTree(arguments));
                if (!errors.isEmpty()) {
                    log.error("[Tool] 파라미터 유효성 검증 실패: {}", errors);
                    Map<String, Object> error = new HashMap<>();
                    error.put("jsonrpc", "2.0");
                    List<String> errorMessages = new ArrayList<>();
                    for (ValidationMessage vm : errors) {
                        errorMessages.add(vm.getMessage());
                    }
                    error.put("error", Map.of("code", -32602, "message", "파라미터 유효성 검증 실패: " + String.join(", ", errorMessages)));
                    error.put("id", payload != null ? payload.get("id") : null);
                    return error;
                }
            } catch (Exception e) {
                log.error("[Tool] 스키마 검증 중 오류 발생: {}", e.getMessage());
            }
        }

        // 3. 대상 Bean 찾기 (ApplicationContext 활용)
        Object targetBean = null;
        Map<String, Object> toolBeans = applicationContext.getBeansWithAnnotation(McpTool.class);
        for (Object bean : toolBeans.values()) {
            McpTool mcpToolAnnotation = bean.getClass().getAnnotation(McpTool.class);
            if (mcpToolAnnotation != null && mcpToolAnnotation.name().equals(toolName)) {
                targetBean = bean;
                break;
            }
        }

        if (targetBean == null) {
            log.error("[Tool] 실행할 Tool Bean을 찾을 수 없습니다: {}", toolName);
            Map<String, Object> error = new HashMap<>();
            error.put("jsonrpc", "2.0");
            error.put("error", Map.of("code", -32601, "message", "실행할 Tool Bean을 찾을 수 없습니다: " + toolName));
            error.put("id", payload != null ? payload.get("id") : null);
            return error;
        }

        // 4. 리플렉션을 통해 대상 메서드 직접 호출
        String actionName = targetFunction.getName();
        Method targetMethod = null;
        for (Method method : targetBean.getClass().getDeclaredMethods()) {
            McpFunction mcpFunc = method.getAnnotation(McpFunction.class);
            if (mcpFunc != null && mcpFunc.name().equals(actionName)) {
                targetMethod = method;
                break;
            }
        }

        if (targetMethod == null) {
            log.error("[Tool] 실행할 함수(Method)를 찾을 수 없습니다: {}", actionName);
            Map<String, Object> error = new HashMap<>();
            error.put("jsonrpc", "2.0");
            error.put("error", Map.of("code", -32601, "message", "실행할 함수(Method)를 찾을 수 없습니다: " + actionName));
            error.put("id", payload != null ? payload.get("id") : null);
            return error;
        }

        log.info("[Tool] 리플렉션 직접 실행 -> Tool: {}, Method: {}", toolName, targetMethod.getName());

        try {
            // 5. DTO 파라미터 자동 매핑 (Map -> DTO)
            Object invokeArgument = arguments;
            if (targetMethod.getParameterCount() > 0 && arguments != null) {
                Class<?> paramType = targetMethod.getParameterTypes()[0];
                if (!Map.class.isAssignableFrom(paramType)) {
                    invokeArgument = objectMapper.convertValue(arguments, paramType);
                    log.info("[Tool] DTO 자동 매핑 성공: {}", paramType.getSimpleName());
                }
            }

            // 6. 메서드 실행
            Object methodResult = targetMethod.invoke(targetBean, invokeArgument);
            
            // 7. 결과 조립 (JSON-RPC 응답)
            Map<String, Object> resultPayload = new HashMap<>();
            if (methodResult instanceof Map) {
                resultPayload = (Map<String, Object>) methodResult;
            } else {
                resultPayload.put("status", "SUCCESS");
                resultPayload.put("legacy_response", methodResult);
            }
            resultPayload.put("ai_insight", "다이렉트 메서드(" + targetMethod.getName() + ") 통신이 성공적으로 수행되었습니다.");

            Map<String, Object> rpcResponse = new HashMap<>();
            rpcResponse.put("jsonrpc", "2.0");
            rpcResponse.put("result", resultPayload);
            rpcResponse.put("id", payload != null ? payload.get("id") : null);
            return rpcResponse;

        } catch (Exception e) {
            log.error("[Tool] 리플렉션 실행 중 예외 발생: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("jsonrpc", "2.0");
            error.put("error", Map.of("code", -32000, "message", "메서드 실행 중 예외 발생: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage())));
            error.put("id", payload != null ? payload.get("id") : null);
            return error;
        }
    }
}
