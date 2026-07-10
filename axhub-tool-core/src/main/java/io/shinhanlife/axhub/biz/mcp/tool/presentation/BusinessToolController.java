package io.shinhanlife.axhub.biz.mcp.tool.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
import io.shinhanlife.axhub.biz.mcp.tool.config.McpProperties;
import io.shinhanlife.axhub.biz.mcp.tool.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.tool.service.ToolRegistryHeartbeatSender;
import java.lang.reflect.Method;
import io.shinhanlife.axhub.biz.mcp.tool.util.JsonSchemaGenerator;
import org.springframework.util.ClassUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.presentation
 * @className BusinessToolController
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class BusinessToolController {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final McpProperties mcpProperties;
    private final ToolRegistryHeartbeatSender toolRegistryHeartbeatSender;

    // 내부 조회용 로컬 Tool 목록 엔드포인트
    @GetMapping("/mcp/api/v1/tools/local")
    public List<ToolMetadata> getLocalTools() {
        return toolRegistryHeartbeatSender.getAllScannedTools();
    }

    // JSON RPC 기반 단일 라우팅 엔드포인트
    @PostMapping("/mcp/api/v1/tools/call")
    public org.springframework.http.ResponseEntity<Map<String, Object>> executeDynamicTool(@RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> params = payload != null ? (Map<String, Object>) payload.get("params") : null;
        String functionName = params != null ? (String) params.get("name") : null;
        
        log.info("\n [Tool] 동적 툴 실행 요청 수신 (함수명): {}", functionName);
        if (payload != null) {
            try {
                log.info(" [Tool] 호출 파라미터: {}", objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                log.info(" [Tool] 호출 파라미터: {}", payload);
            }
        }

        Map<String, Object> arguments = params != null ? (Map<String, Object>) params.get("arguments") : null;
        Object targetBean = null;
        Method targetMethod = null;
        McpFunction targetFunctionAnnotation = null;
        
        // McpTool 어노테이션 기반 조회가 프록시 문제로 누락될 수 있으므로, 전체 빈을 순회하며 @McpFunction을 찾습니다.
        Map<String, Object> allBeans = applicationContext.getBeansOfType(Object.class);
        outerLoop:
        for (Object bean : allBeans.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getDeclaredMethods()) {
                McpFunction mcpFunc = AnnotationUtils.findAnnotation(method, McpFunction.class);
                if (mcpFunc != null) {
                    String baseName = mcpFunc.name();
                    String expectedName = mcpProperties.getNamespace() != null && !mcpProperties.getNamespace().isEmpty()
                            ? mcpProperties.getNamespace() + "_" + baseName
                            : baseName;
                    
                    if (expectedName.equals(functionName) || baseName.equals(functionName)) {
                        targetBean = bean;
                        targetMethod = method;
                        targetFunctionAnnotation = mcpFunc;
                        break outerLoop;
                    }
                }
            }
        }

        if (targetBean == null || targetMethod == null) {
            List<String> availableFunctions = new ArrayList<>();
            for (Object bean : allBeans.values()) {
                Class<?> targetCls = AopUtils.getTargetClass(bean);
                for (Method m : targetCls.getDeclaredMethods()) {
                    McpFunction func = AnnotationUtils.findAnnotation(m, McpFunction.class);
                    if (func != null) {
                        String baseName = func.name();
                        String expName = mcpProperties.getNamespace() != null && !mcpProperties.getNamespace().isEmpty()
                                ? mcpProperties.getNamespace() + "_" + baseName : baseName;
                        availableFunctions.add(expName + " (in " + targetCls.getSimpleName() + ")");
                    }
                }
            }
            log.error("[Tool] 실행할 함수(Method)를 찾을 수 없습니다: {}. 현재 스캔된 툴 메서드 목록: {}", functionName, availableFunctions);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("status", "404");
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("code", "TOOL_NOT_FOUND");
            errorBody.put("message", "실행할 함수를 찾을 수 없습니다: " + functionName);
            errorBody.put("details", errorDetails);

            Map<String, Object> error = new HashMap<>();
            error.put("jsonrpc", "2.0");
            error.put("error", errorBody);
            error.put("id", payload != null ? payload.get("id") : null);
            return org.springframework.http.ResponseEntity.ok(error);
        }
        // (기존 차단 로직 제거됨)

        // 2. 파라미터 유효성 검증 (JSON Schema)
        if (targetMethod.getParameterCount() > 0) {
            Class<?> paramType = targetMethod.getParameterTypes()[0];
            if (!Map.class.isAssignableFrom(paramType)) {
                try {
                    Map<String, Object> autoSchema = JsonSchemaGenerator.generatePropertiesSchema(paramType);
                    String fullSchemaJson = "{\"type\":\"object\", \"properties\":" + objectMapper.writeValueAsString(autoSchema) + "}";
                    
                    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
                    JsonSchema schema = factory.getSchema(fullSchemaJson);
                    
                    Set<ValidationMessage> errors = schema.validate(objectMapper.valueToTree(arguments));
                    if (!errors.isEmpty()) {
                        log.error("[Tool] 파라미터 유효성 검증 실패: {}", errors);
                        List<String> errorMessages = new ArrayList<>();
                        for (ValidationMessage vm : errors) {
                            errorMessages.add(vm.getMessage());
                        }
                        Map<String, Object> errorDetails = new HashMap<>();
                        errorDetails.put("status", "422");
                        Map<String, Object> errorBody = new HashMap<>();
                        errorBody.put("code", "INVALID_PARAM");
                        errorBody.put("message", "파라미터 유효성 검증 실패: " + String.join(", ", errorMessages));
                        errorBody.put("details", errorDetails);

                        Map<String, Object> error = new HashMap<>();
                        error.put("jsonrpc", "2.0");
                        error.put("error", errorBody);
                        error.put("id", payload != null ? payload.get("id") : null);
                        return org.springframework.http.ResponseEntity.ok(error);
                    }
                } catch (Exception e) {
                    log.error("[Tool] 스키마 검증 중 오류 발생: {}", e.getMessage());
                }
            }
        }

        log.info("[Tool] 리플렉션 직접 실행 -> Method: {}", targetMethod.getName());

        try {
            // 3. DTO 파라미터 자동 매핑 (Map -> DTO)
            Object invokeArgument = arguments;
            if (targetMethod.getParameterCount() > 0 && arguments != null) {
                Class<?> paramType = targetMethod.getParameterTypes()[0];
                if (!Map.class.isAssignableFrom(paramType)) {
                    invokeArgument = objectMapper.convertValue(arguments, paramType);
                    log.info("[Tool] DTO 자동 매핑 성공: {}", paramType.getSimpleName());
                }
            }

            // 4. 메서드 실행
            
            long startTime = System.currentTimeMillis();
            Object methodResult = null;
            final Object finalTargetBean = targetBean;
            final Object finalInvokeArgument = invokeArgument;
            final java.lang.reflect.Method finalTargetMethod = targetMethod;
            int timeoutMs = targetFunctionAnnotation != null ? targetFunctionAnnotation.timeoutMs() : 300000;
            
            try {
                methodResult = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        return finalTargetMethod.invoke(finalTargetBean, finalInvokeArgument);
                    } catch (Exception ex) {
                        throw new java.util.concurrent.CompletionException(ex);
                    }
                }).get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException te) {
                long elapsed = System.currentTimeMillis() - startTime;
                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("code", "TOOLBOX_EXEC_TIMEOUT");
                errorBody.put("message", "Tool execution timed out after " + timeoutMs + " ms");
                Map<String, Object> error = new HashMap<>();
                error.put("jsonrpc", "2.0");
                error.put("error", errorBody);
                error.put("id", payload != null ? payload.get("id") : null);
                
                Map<String, Object> resultPayload = new HashMap<>();
                resultPayload.put("status", "timeout");
                resultPayload.put("result", null);
                resultPayload.put("error_code", "TOOLBOX_EXEC_TIMEOUT");
                resultPayload.put("error_message", "Tool execution timed out");
                resultPayload.put("elapsed_ms", elapsed);
                resultPayload.put("truncated", false);
                resultPayload.put("original_size", 0);
                
                error.put("result", resultPayload);
                return org.springframework.http.ResponseEntity.status(504).body(error);
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            
            // 5. 결과 조립 (JSON-RPC 응답 - Agent Builder 규격 적용)
            Map<String, Object> innerResult = new HashMap<>();
            if (methodResult instanceof Map) {
                innerResult.putAll((Map<String, Object>) methodResult);
            }
            if (!innerResult.containsKey("contracts")) {
                List<Object> contracts = new ArrayList<>();
                if (methodResult != null) {
                    contracts.add(methodResult);
                }
                innerResult.put("contracts", contracts);
            }

            Map<String, Object> resultPayload = new HashMap<>();
            resultPayload.put("status", "ok");
            resultPayload.put("result", innerResult);
            resultPayload.put("error_code", null);
            resultPayload.put("error_message", null);
            resultPayload.put("elapsed_ms", elapsed);
            resultPayload.put("truncated", false);
            resultPayload.put("original_size", 0);

            Map<String, Object> rpcResponse = new java.util.LinkedHashMap<>(); // 순서 보장을 위해 LinkedHashMap 사용
            rpcResponse.put("jsonrpc", "2.0");
            rpcResponse.put("result", resultPayload);
            rpcResponse.put("id", payload != null ? payload.get("id") : null);
            
            try {
                log.info("[Tool -> MCP Gateway] 동적 툴 실행 결과 반환: {}", objectMapper.writeValueAsString(rpcResponse));
            } catch (Exception e) {
                log.info("[Tool -> MCP Gateway] 동적 툴 실행 결과 반환: {}", rpcResponse);
            }
            
            try {
                String resultJson = objectMapper.writeValueAsString(innerResult);
                int size = resultJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                resultPayload.put("original_size", size);
                
                if (size > 1048576) {
                    Map<String, Object> errorBody = new HashMap<>();
                    errorBody.put("code", "TOOLBOX_RESPONSE_TOO_LARGE");
                    errorBody.put("message", "Response size exceeds 1 MiB limit");
                    Map<String, Object> error = new HashMap<>();
                    error.put("jsonrpc", "2.0");
                    error.put("error", errorBody);
                    error.put("id", payload != null ? payload.get("id") : null);
                    
                    resultPayload.put("status", "error");
                    resultPayload.put("result", null);
                    resultPayload.put("error_code", "TOOLBOX_RESPONSE_TOO_LARGE");
                    resultPayload.put("error_message", "Response size exceeds 1 MiB limit");
                    error.put("result", resultPayload);
                    return org.springframework.http.ResponseEntity.status(413).body(error);
                } else if (size > 30000) {
                    String truncatedStr = resultJson.substring(0, 30000) + "... (truncated)";
                    resultPayload.put("result", truncatedStr);
                    resultPayload.put("truncated", true);
                }
            } catch (Exception e) {
                log.warn("Failed to measure size", e);
            }
            return org.springframework.http.ResponseEntity.ok(rpcResponse);

        } catch (Exception e) {
            log.error("[Tool] 리플렉션 실행 중 예외 발생: {}", e.getMessage());
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("status", "500");
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("code", "TOOL_ERROR");
            errorBody.put("message", "메서드 실행 중 예외 발생: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            errorBody.put("details", errorDetails);

            Map<String, Object> error = new HashMap<>();
            error.put("jsonrpc", "2.0");
            error.put("error", errorBody);
            error.put("id", payload != null ? payload.get("id") : null);
            return org.springframework.http.ResponseEntity.ok(error);
        }
    }
}