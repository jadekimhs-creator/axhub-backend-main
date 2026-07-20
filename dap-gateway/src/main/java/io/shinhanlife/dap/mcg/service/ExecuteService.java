package io.shinhanlife.dap.mcg.service;


/**
 * @package io.shinhanlife.dap.mcg.service
 * @className ExecuteService
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
import java.util.HashMap;

import io.shinhanlife.dap.mcg.dto.ToolMetadata;
import io.shinhanlife.dap.mcg.resilience.FailureType;
import io.shinhanlife.dap.mcg.resilience.RetryPolicy;
import io.shinhanlife.dap.mcg.resilience.ToolExecutionException;
import io.shinhanlife.dap.mcg.dto.OperationType;
import io.shinhanlife.dap.mcg.config.McpGatewayProperties;
import io.shinhanlife.dap.mcg.guardrail.SensitiveDataMasker;
import io.shinhanlife.dap.mcg.guardrail.GuardrailService;
import io.shinhanlife.dap.mcg.security.McpRequestContext;
import io.shinhanlife.dap.mcg.security.McpRequestContextResolver;
import io.shinhanlife.dap.mcg.audit.AuditLogService;
import io.shinhanlife.dap.mcg.resilience.CircuitBreaker;
import io.shinhanlife.dap.mcg.resilience.CircuitBreakerService;
import io.shinhanlife.dap.mcg.security.ToolAuthorizationService;
import io.shinhanlife.dap.mcg.redis.RedisToolTraceService;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.shinhanlife.dap.mcg.tool.large.LargeToolResponseService;
import io.shinhanlife.dap.mcg.tool.large.PaginationRequestValidator;
import io.shinhanlife.dap.mcg.tool.result.ToolExecutionResultFormatter;
import io.shinhanlife.dap.mcg.tool.result.ToolExecutionResult;
import io.shinhanlife.dap.mcg.guardrail.ToolResponseGuardrailService;
import io.shinhanlife.dap.mcg.transport.ToolInvoker;

@Slf4j
@Service
public class ExecuteService {

    private final ToolPlanner planner;
    private final KillSwitchService killSwitchService;
    private final ObjectMapper objectMapper;
    private final SensitiveDataMasker dataMasker;
    private final GuardrailService guardrailService;
    private final McpRequestContextResolver contextResolver;
    private final AuditLogService auditLogService;
    private final CircuitBreakerService circuitBreakerService;
    private final ToolAuthorizationService authorizationService;
    private final RedisToolTraceService redisTrace;
    private final McpGatewayProperties properties;
    private final LargeToolResponseService largeResponses;
    private final PaginationRequestValidator paginationValidator;
    private final ToolExecutionResultFormatter resultFormatter;
    private final ToolResponseGuardrailService responseGuardrail;
    private final ToolInvoker toolInvoker;
    private final ExecutorService executor;

    public ExecuteService(ToolPlanner planner,
                          KillSwitchService killSwitchService,
                          ObjectMapper objectMapper,
                          SensitiveDataMasker dataMasker,
                          GuardrailService guardrailService,
                          McpRequestContextResolver contextResolver,
                          AuditLogService auditLogService,
                          CircuitBreakerService circuitBreakerService,
                          ToolAuthorizationService authorizationService,
                          RedisToolTraceService redisTrace,
                          McpGatewayProperties properties,
                          LargeToolResponseService largeResponses,
                          PaginationRequestValidator paginationValidator,
                          ToolExecutionResultFormatter resultFormatter,
                          ToolResponseGuardrailService responseGuardrail,
                          ToolInvoker toolInvoker) {
        this.planner = planner;
        this.killSwitchService = killSwitchService;
        this.objectMapper = objectMapper;
        this.dataMasker = dataMasker;
        this.guardrailService = guardrailService;
        this.contextResolver = contextResolver;
        this.auditLogService = auditLogService;
        this.circuitBreakerService = circuitBreakerService;
        this.authorizationService = authorizationService;
        this.redisTrace = redisTrace;
        this.properties = properties;
        this.largeResponses = largeResponses;
        this.paginationValidator = paginationValidator;
        this.resultFormatter = resultFormatter;
        this.responseGuardrail = responseGuardrail;
        this.toolInvoker = toolInvoker;
        
        this.executor = new ThreadPoolExecutor(
                properties.toolExecutorCorePoolSize(),
                properties.toolExecutorMaxPoolSize(),
                properties.toolExecutorKeepAliveSeconds(),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(properties.toolExecutorQueueCapacity()),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public Object execute(Map<String, Object> payload, String tenantId) {
        log.info(" [ExecuteService] 전체 실행 흐름 제어 시작");

        killSwitchService.checkAgent(tenantId);

        Map<String, Object> params = payload.containsKey("params") ? (Map<String, Object>) payload.get("params") : null;
        String toolName = params != null ? (String) params.get("name") : (String) payload.get("toolName");
        killSwitchService.checkTool(toolName);

        var planObj = planner.createPlan(payload, tenantId);
        ToolMetadata metadata = (ToolMetadata) planObj;

        McpRequestContext context = contextResolver.current();

        ObjectNode argumentsNode = objectMapper.createObjectNode();
        if (params != null && params.containsKey("arguments")) {
            argumentsNode = objectMapper.valueToTree(params.get("arguments"));
        }

        long startedAt = System.nanoTime();
        auditLogService.toolStarted(context, toolName, argumentsNode);
        redisTrace.started(context, metadata, argumentsNode);

        try {
            authorizationService.authorize(context, metadata);
            if (params != null && params.containsKey("arguments")) {
                guardrailService.validate(metadata, argumentsNode);
            }

            if (metadata.getIntegrationType() != null) {
                killSwitchService.checkRoute(metadata.getIntegrationType());
            }

            Object result = executeWithResilience(context, metadata, argumentsNode, payload);
            
            if (result instanceof com.fasterxml.jackson.databind.JsonNode) {
                // Apply Output Guardrail
                String wrappedResponse = responseGuardrail.validateAndWrap(metadata, context.requestId(), (com.fasterxml.jackson.databind.JsonNode) result);
                
                // Format the result (Restore legacy raw format as Map/List for backward compatibility)
                result = objectMapper.readValue(wrappedResponse, Object.class);
            }
            
            long elapsedMillis = elapsedMillis(startedAt);
            
            String responseText = "";
            try { responseText = objectMapper.writeValueAsString(result); } catch (Exception ignore) {}
            
            auditLogService.toolFinished(context, metadata.getName(), elapsedMillis, true, "");
            redisTrace.finished(context, metadata, argumentsNode, elapsedMillis, true, "", responseText);
            
            return result;
        } catch (ToolExecutionException error) {
            long elapsedMillis = elapsedMillis(startedAt);
            auditLogService.toolFinished(context, toolName, elapsedMillis, false, error.failureType().name());
            redisTrace.finished(context, metadata, argumentsNode, elapsedMillis, false, error.failureType().name(), "");
            throw error;
        } catch (Exception error) {
            long elapsedMillis = elapsedMillis(startedAt);
            auditLogService.toolFinished(context, toolName, elapsedMillis, false, FailureType.INTERNAL_ERROR.name());
            redisTrace.finished(context, metadata, argumentsNode, elapsedMillis, false, FailureType.INTERNAL_ERROR.name(), "");
            throw new ToolExecutionException(FailureType.INTERNAL_ERROR, "Tool execution failed: " + toolName, error);
        }
    }

    private Object executeWithResilience(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments, Map<String, Object> payload) {
        RetryPolicy retryPolicy = retryPolicy(metadata, arguments);
        
        int failureThreshold = (metadata.getCircuitBreakerFailureThreshold() != null && metadata.getCircuitBreakerFailureThreshold() > 0)
                ? metadata.getCircuitBreakerFailureThreshold()
                : properties.circuitBreakerFailureThreshold();
                
        long openMillis = (metadata.getCircuitBreakerOpenMillis() != null && metadata.getCircuitBreakerOpenMillis() > 0)
                ? metadata.getCircuitBreakerOpenMillis()
                : properties.circuitBreakerOpenMillis();
                
        CircuitBreaker circuitBreaker = circuitBreakerService.breaker("tool:" + metadata.getName(),
                failureThreshold, openMillis);
                
        ToolExecutionException lastError = null;
        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            try {
                redisTrace.attemptStarted(context, metadata, arguments, attempt, retryPolicy.maxAttempts());
                circuitBreaker.beforeCall();
                Object result = executeOnce(context, metadata, arguments, payload);
                circuitBreaker.recordSuccess();
                return result;
            } catch (ToolExecutionException error) {
                lastError = error;
                circuitBreaker.recordFailure(error.failureType());
                if (!shouldRetry(metadata, arguments, error.failureType(), attempt, retryPolicy)) {
                    throw error;
                }
                long backoffMillis = retryPolicy.backoffMillis(attempt);
                redisTrace.retryWaiting(context, metadata, arguments, attempt, retryPolicy.maxAttempts(), backoffMillis,
                        error.failureType().name());
                sleepBeforeRetry(metadata.getName(), attempt, backoffMillis, error.failureType());
            }
        }
        throw lastError == null
                ? new ToolExecutionException(FailureType.INTERNAL_ERROR, "Tool execution failed: " + metadata.getName())
                : lastError;
    }

    private Object executeOnce(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments, Map<String, Object> payload) {
        CompletableFuture<Object> future;

        try {
            future = CompletableFuture.supplyAsync(() -> {
                try {
                    String targetUrl = "http://localhost:8084"; // Fallback
                    if (metadata.getPodUrl() != null && !metadata.getPodUrl().isEmpty()) {
                        targetUrl = metadata.getPodUrl();
                    }
                    String executeApiUrl = targetUrl + "/mcp/api/v1/tools/call";
                    
                    ObjectNode pageArguments = paginationValidator.normalize(arguments);
                    LargeToolResponseService.Collector collector = largeResponses.newCollector(metadata.getName(), context.requestId());

                    while (true) {
                        Map<String, Object> pagePayload = new HashMap<>(payload);
                        if (pagePayload.containsKey("params")) {
                            Map<String, Object> params = new HashMap<>((Map<String, Object>) pagePayload.get("params"));
                            params.put("arguments", objectMapper.convertValue(pageArguments, Map.class));
                            pagePayload.put("params", params);
                        } else {
                            pagePayload.put("arguments", objectMapper.convertValue(pageArguments, Map.class));
                        }
                        
                        try {
                            log.info(" [ExecuteService] 요청 페이로드(마스킹 적용): {}", objectMapper.writeValueAsString(dataMasker.mask(objectMapper.valueToTree(pagePayload))));
                        } catch (Exception ignore) {}
                        
                        JsonNode data = null;
                        try {
                            data = toolInvoker.invoke(pagePayload, executeApiUrl);
                        } catch (Exception e) {
                            if (executeApiUrl.contains("http://tool-")) {
                                String fallbackUrl = executeApiUrl.replaceAll("http://tool-[a-zA-Z0-9-]+", "http://localhost");
                                log.warn(" [ExecuteService] 호스트를 찾을 수 없어 localhost로 재시도합니다: {}", fallbackUrl);
                                try {
                                    data = toolInvoker.invoke(pagePayload, fallbackUrl);
                                } catch (Exception ex) {
                                    throw new ToolExecutionException(FailureType.SERVER_ERROR, "Tool Pod 호출 실패 (localhost 재시도 포함): " + ex.getMessage());
                                }
                            } else {
                                throw new ToolExecutionException(FailureType.SERVER_ERROR, "Tool Pod 호출 실패: " + e.getMessage());
                            }
                        }

                        collector.accept(data);
                        if (!collector.shouldFetchNextPage()) {
                            return collector.finish();
                        }
                        pageArguments.put("cursor", collector.nextCursor());
                        pageArguments.put("pageSize", collector.pageSize());
                    }
                } catch (ToolExecutionException error) {
                    throw error;
                } catch (Exception error) {
                    throw new ToolExecutionException(
                            FailureType.INTERNAL_ERROR,
                            "Tool execution failed: " + metadata.getName(),
                            error
                    );
                }
            }, executor);
        } catch (RuntimeException error) {
            throw new ToolExecutionException(
                    FailureType.SERVER_ERROR,
                    "MCP Tool 실행 큐가 가득 찼습니다: " + metadata.getName(),
                    error
            );
        }

        try {
            return future.get(timeoutMillis(metadata), TimeUnit.MILLISECONDS);
        } catch (TimeoutException error) {
            future.cancel(true);
            throw new ToolExecutionException(FailureType.TIMEOUT, "Tool execution timed out: " + metadata.getName(), error);
        } catch (ExecutionException error) {
            if (error.getCause() instanceof ToolExecutionException toolError) {
                throw toolError;
            }
            throw new ToolExecutionException(FailureType.INTERNAL_ERROR, "Tool execution failed: " + metadata.getName(), error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ToolExecutionException(FailureType.INTERNAL_ERROR, "Tool 실행이 중단되었습니다: " + metadata.getName(), error);
        }
    }
    
    private RetryPolicy retryPolicy(ToolMetadata metadata, ObjectNode arguments) {
        if (!retryAllowedByOperation(metadata, arguments)) {
            return RetryPolicy.disabled();
        }
        return new RetryPolicy(properties.retryMaxAttempts(), properties.retryInitialBackoffMillis(),
                properties.retryBackoffMultiplier(), properties.retryMaxBackoffMillis());
    }

    private boolean retryAllowedByOperation(ToolMetadata metadata, ObjectNode arguments) {
        if (metadata.getOperationType() == OperationType.READ) {
            return metadata.getRetryEnabled() != null ? metadata.getRetryEnabled() : true;
        }
        return arguments.hasNonNull("idempotencyKey") && !arguments.path("idempotencyKey").asText("").isBlank();
    }

    private boolean shouldRetry(ToolMetadata metadata, ObjectNode arguments, FailureType failureType, int attempt, RetryPolicy retryPolicy) {
        if (attempt >= retryPolicy.maxAttempts() || !retryAllowedByOperation(metadata, arguments)) {
            return false;
        }
        return failureType == FailureType.TIMEOUT || failureType == FailureType.NETWORK_ERROR || failureType == FailureType.SERVER_ERROR;
    }

    private void sleepBeforeRetry(String toolName, int attempt, long backoffMillis, FailureType failureType) {
        if (backoffMillis <= 0) {
            return;
        }
        log.warn("Retrying tool call. tool={} failedAttempt={} failureType={} backoffMillis={}", toolName, attempt, failureType, backoffMillis);
        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ToolExecutionException(FailureType.INTERNAL_ERROR, "Tool 재시도가 중단되었습니다: " + toolName, error);
        }
    }

    private long timeoutMillis(ToolMetadata metadata) {
        return (metadata.getTimeoutMillis() != null && metadata.getTimeoutMillis() > 0) ? metadata.getTimeoutMillis() : properties.toolTimeoutMillis();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}