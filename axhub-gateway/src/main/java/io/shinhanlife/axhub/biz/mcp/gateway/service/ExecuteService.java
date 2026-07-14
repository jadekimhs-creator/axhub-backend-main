package io.shinhanlife.axhub.biz.mcp.gateway.service;

import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.gateway.resilience.FailureType;
import io.shinhanlife.axhub.biz.mcp.gateway.resilience.RetryPolicy;
import io.shinhanlife.axhub.biz.mcp.gateway.resilience.ToolExecutionException;
import io.shinhanlife.axhub.biz.mcp.gateway.dto.OperationType;
import io.shinhanlife.axhub.biz.mcp.gateway.config.McpGatewayProperties;
import io.shinhanlife.axhub.biz.mcp.gateway.guardrail.SensitiveDataMasker;
import io.shinhanlife.axhub.biz.mcp.gateway.guardrail.GuardrailService;
import io.shinhanlife.axhub.biz.mcp.gateway.security.McpRequestContext;
import io.shinhanlife.axhub.biz.mcp.gateway.security.McpRequestContextResolver;
import io.shinhanlife.axhub.biz.mcp.gateway.audit.AuditLogService;
import io.shinhanlife.axhub.biz.mcp.gateway.resilience.CircuitBreaker;
import io.shinhanlife.axhub.biz.mcp.gateway.resilience.CircuitBreakerService;
import io.shinhanlife.axhub.biz.mcp.gateway.security.ToolAuthorizationService;
import io.shinhanlife.axhub.biz.mcp.gateway.redis.RedisToolTraceService;

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
import io.shinhanlife.axhub.biz.mcp.gateway.tool.large.LargeToolResponseService;
import io.shinhanlife.axhub.biz.mcp.gateway.tool.large.PaginationRequestValidator;
import io.shinhanlife.axhub.biz.mcp.gateway.tool.result.ToolExecutionResultFormatter;
import io.shinhanlife.axhub.biz.mcp.gateway.tool.result.ToolExecutionResult;
import io.shinhanlife.axhub.biz.mcp.gateway.guardrail.ToolResponseGuardrailService;
import io.shinhanlife.axhub.biz.mcp.gateway.transport.ToolInvoker;

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
                
                // Format the result
                ToolExecutionResult formattedResult = resultFormatter.fromRawResponse(metadata.getName(), wrappedResponse);
                result = formattedResult;
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
        CircuitBreaker circuitBreaker = circuitBreakerService.breaker("tool:" + metadata.getName(),
                metadata.getCircuitBreakerFailureThreshold(),
                metadata.getCircuitBreakerOpenMillis());
                
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
                        Map<String, Object> pagePayload = new java.util.HashMap<>(payload);
                        if (pagePayload.containsKey("params")) {
                            Map<String, Object> params = new java.util.HashMap<>((Map<String, Object>) pagePayload.get("params"));
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