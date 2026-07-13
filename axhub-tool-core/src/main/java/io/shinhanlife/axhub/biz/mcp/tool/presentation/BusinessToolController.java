package io.shinhanlife.axhub.biz.mcp.tool.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.config.McpProperties;
import io.shinhanlife.axhub.biz.mcp.tool.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.tool.service.ToolRegistryHeartbeatSender;
import io.shinhanlife.axhub.biz.mcp.tool.util.JsonSchemaGenerator;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Map<String, Object>> executeDynamicTool(
            @RequestHeader(value = "X-Request-Id", required = false) String headerRequestId,
            @RequestBody(required = false) Map<String, Object> payload) {
        
        String finalRequestId = headerRequestId != null ? headerRequestId : (payload != null && payload.get("id") != null ? String.valueOf(payload.get("id")) : null);
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
            if (finalRequestId != null) errorBody.put("request_id", finalRequestId);

            Map<String, Object> error = new HashMap<>();
            error.put("jsonrpc", "2.0");
            error.put("error", errorBody);
            error.put("id", payload != null ? payload.get("id") : null);
            return ResponseEntity.status(404).body(error);
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
                        errorBody.put("message", "파라미터 유효성 검증 실패");
                        errorBody.put("details", errorDetails);
                        if (finalRequestId != null) errorBody.put("request_id", finalRequestId);

                        Map<String, Object> error = new HashMap<>();
                        error.put("jsonrpc", "2.0");
                        error.put("error", errorBody);
                        error.put("id", payload != null ? payload.get("id") : null);
                        return ResponseEntity.status(422).body(error);
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
            methodResult = targetMethod.invoke(targetBean, invokeArgument);
            
            long elapsed = System.currentTimeMillis() - startTime;
            
            // 5. 결과 조립 (JSON-RPC 응답 - Agent Builder 규격 적용)
            Map<String, Object> innerResult = new HashMap<>();
            if (methodResult instanceof Map && ((Map<?, ?>) methodResult).containsKey("contracts")) {
                innerResult.putAll((Map<String, Object>) methodResult);
            } else {
                List<Object> contracts = new ArrayList<>();
                if (methodResult != null) {
                    contracts.add(methodResult);
                }
                innerResult.put("contracts", contracts);
                
                if (methodResult instanceof Map && ((Map<?, ?>) methodResult).containsKey("status")) {
                    innerResult.put("status", ((Map<?, ?>) methodResult).get("status"));
                } else {
                    innerResult.put("status", "SUCCESS");
                }
            }

            Map<String, Object> resultPayload = new HashMap<>();
            resultPayload.put("status", "ok");
            resultPayload.put("result", innerResult);
            resultPayload.put("error_code", null);
            resultPayload.put("error_message", null);
            resultPayload.put("elapsed_ms", elapsed);
            resultPayload.put("truncated", false);
                        int originalSize = 0;
            try {
                if (methodResult instanceof Map && ((Map<?, ?>) methodResult).containsKey("legacy_response")) {
                    Object legacyResp = ((Map<?, ?>) methodResult).get("legacy_response");
                    if (legacyResp instanceof String) {
                        originalSize = ((String) legacyResp).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                    }
                } else {
                    String jsonStr = objectMapper.writeValueAsString(innerResult);
                    originalSize = jsonStr.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                }
            } catch (Exception e) {
                log.warn("Failed to calculate original_size", e);
            }
            resultPayload.put("original_size", originalSize);

            Map<String, Object> rpcResponse = new LinkedHashMap<>(); // 순서 보장을 위해 LinkedHashMap 사용
            rpcResponse.put("jsonrpc", "2.0");
            rpcResponse.put("result", resultPayload);
            rpcResponse.put("id", payload != null ? payload.get("id") : null);
            
            try {
                log.info("[Tool -> MCP Gateway] 동적 툴 실행 결과 반환: {}", objectMapper.writeValueAsString(rpcResponse));
            } catch (Exception e) {
                log.info("[Tool -> MCP Gateway] 동적 툴 실행 결과 반환: {}", rpcResponse);
            }
            
            
            return ResponseEntity.ok(rpcResponse);

        } catch (Exception e) {
            log.error("[Tool] 리플렉션 실행 중 예외 발생: {}", e.getMessage());
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("status", "500");
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("code", "TOOL_ERROR");
            errorBody.put("message", "Tool execution failed");
            errorDetails.clear(); // Hide details for upstream errors
            errorBody.put("details", errorDetails);
            if (finalRequestId != null) errorBody.put("request_id", finalRequestId);

            Map<String, Object> error = new HashMap<>();
            error.put("jsonrpc", "2.0");
            error.put("error", errorBody);
            error.put("id", payload != null ? payload.get("id") : null);
            return ResponseEntity.status(502).body(error);
        }
    }
}