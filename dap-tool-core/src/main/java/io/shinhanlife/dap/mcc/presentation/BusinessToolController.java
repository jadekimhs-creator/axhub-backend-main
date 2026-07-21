package io.shinhanlife.dap.mcc.presentation;


/**
 * @package io.shinhanlife.dap.mcc.presentation
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import io.shinhanlife.dap.mcc.config.McpProperties;
import io.shinhanlife.dap.mcc.dto.ToolMetadata;
import io.shinhanlife.dap.mcc.service.ToolRegistryHeartbeatSender;
import io.shinhanlife.dap.mcc.util.JsonSchemaGenerator;
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

    // 순수 REST 기반 동적 라우팅 엔드포인트
    @PostMapping("/mcp/{name}")
    public ResponseEntity<?> executeDynamicTool(
            @PathVariable("name") String functionName,
            @RequestHeader(value = "X-Request-Id", required = false) String headerRequestId,
            @RequestBody(required = false) Map<String, Object> arguments) {
        
        String finalRequestId = headerRequestId;
        
        log.info("\n [Tool] 동적 툴 실행 요청 수신 (함수명): {}", functionName);
        if (arguments != null) {
            try {
                log.info(" [Tool] 호출 파라미터: {}", objectMapper.writeValueAsString(arguments));
            } catch (Exception e) {
                log.info(" [Tool] 호출 파라미터: {}", arguments);
            }
        }
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

            return ResponseEntity.status(404).body(errorBody);
        }
        // (기존 차단 로직 제거됨)

        // 2. 파라미터 유효성 검증 (JSON Schema)
        if (targetMethod.getParameterCount() > 0) {
            Class<?> paramType = targetMethod.getParameterTypes()[0];
            if (!Map.class.isAssignableFrom(paramType)) {
                try {
                    Map<String, Object> autoSchema = JsonSchemaGenerator.generateSchema(paramType);
                    String fullSchemaJson = objectMapper.writeValueAsString(autoSchema);
                    
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

                        return ResponseEntity.status(422).body(errorBody);
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
            
            // 5. 결과 반환 (순수 REST 응답)
            try {
                log.info("[Tool -> MCP Gateway] 동적 툴 실행 결과 반환: {}", objectMapper.writeValueAsString(methodResult));
            } catch (Exception e) {
                log.info("[Tool -> MCP Gateway] 동적 툴 실행 결과 반환: {}", methodResult);
            }
            
            return ResponseEntity.ok(methodResult);

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

            return ResponseEntity.status(502).body(errorBody);
        }
    }
}