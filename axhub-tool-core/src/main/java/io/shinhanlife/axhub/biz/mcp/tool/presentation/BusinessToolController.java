package io.shinhanlife.axhub.biz.mcp.tool.presentation;

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
import io.shinhanlife.axhub.biz.mcp.tool.config.McpProperties;
import java.lang.reflect.Method;
import io.shinhanlife.axhub.biz.mcp.tool.util.JsonSchemaGenerator;
import org.springframework.util.ClassUtils;

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

    // JSON RPC 기반 단일 라우팅 엔드포인트
    @PostMapping("/mcp/api/v1/tools/call")
    public Map<String, Object> executeDynamicTool(@RequestBody(required = false) Map<String, Object> payload) {
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

        // 1. 대상 Bean 및 Method 찾기 (ApplicationContext 활용)
        Map<String, Object> arguments = params != null ? (Map<String, Object>) params.get("arguments") : null;
        Object targetBean = null;
        Method targetMethod = null;
        McpFunction targetFunctionAnnotation = null;

        Map<String, Object> toolBeans = applicationContext.getBeansWithAnnotation(McpTool.class);
        outerLoop:
        for (Object bean : toolBeans.values()) {
            Class<?> userClass = ClassUtils.getUserClass(bean);
            for (Method method : userClass.getDeclaredMethods()) {
                McpFunction mcpFunc = method.getAnnotation(McpFunction.class);
                if (mcpFunc != null) {
                    String baseName = mcpFunc.name();
                    String expectedName = mcpProperties.getNamespace() != null && !mcpProperties.getNamespace().isEmpty()
                            ? mcpProperties.getNamespace() + "_" + baseName
                            : baseName;
                    
                    if (expectedName.equals(functionName)) {
                        targetBean = bean;
                        targetMethod = method;
                        targetFunctionAnnotation = mcpFunc;
                        break outerLoop;
                    }
                }
            }
        }

        if (targetBean == null || targetMethod == null) {
            log.error("[Tool] 실행할 함수(Method)를 찾을 수 없습니다: {}", functionName);
            Map<String, Object> error = new HashMap<>();
            error.put("jsonrpc", "2.0");
            error.put("error", Map.of("code", -32601, "message", "실행할 함수를 찾을 수 없습니다: " + functionName));
            error.put("id", payload != null ? payload.get("id") : null);
            return error;
        }

        // --- 비공개 툴(register=false)은 외부 노출(Redis)만 제외하고, 직접 실행은 허용하도록 변경 ---
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
            Object methodResult = targetMethod.invoke(targetBean, invokeArgument);
            
            // 5. 결과 조립 (JSON-RPC 응답)
            Map<String, Object> resultPayload = new HashMap<>();
            if (methodResult instanceof Map) {
                resultPayload = (Map<String, Object>) methodResult;
            } else {
                resultPayload.put("status", "SUCCESS");
                resultPayload.put("legacy_response", methodResult);
            }
            resultPayload.put("ai_insight", "다이렉트 메서드(" + targetMethod.getName() + ") 통신이 성공적으로 수행되었습니다.");

            Map<String, Object> rpcResponse = new java.util.LinkedHashMap<>(); // 순서 보장을 위해 LinkedHashMap 사용
            rpcResponse.put("jsonrpc", "2.0");
            rpcResponse.put("result", resultPayload);
            rpcResponse.put("id", payload != null ? payload.get("id") : null);
            
            try {
                log.info("[Tool -> MCP Gateway] 동적 툴 실행 결과 반환: {}", objectMapper.writeValueAsString(rpcResponse));
            } catch (Exception e) {
                log.info("[Tool -> MCP Gateway] 동적 툴 실행 결과 반환: {}", rpcResponse);
            }
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