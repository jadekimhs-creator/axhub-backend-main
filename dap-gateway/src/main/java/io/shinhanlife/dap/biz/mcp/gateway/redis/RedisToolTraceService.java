package io.shinhanlife.dap.biz.mcp.gateway.redis;

import io.shinhanlife.dap.biz.mcp.gateway.config.McpGatewayProperties;
import io.shinhanlife.dap.biz.mcp.gateway.guardrail.SensitiveDataMasker;
import io.shinhanlife.dap.biz.mcp.gateway.security.McpRequestContext;
import io.shinhanlife.dap.biz.mcp.gateway.dto.ToolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RedisToolTraceService {
    private static final Logger log = LoggerFactory.getLogger(RedisToolTraceService.class);
    private static final String KEY_PREFIX = "mcp:request:";
    private static final String RECENT_KEY = "mcp:request:recent";

    private final McpGatewayProperties properties;
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectMapper json;
    private final McpMonitorEventService monitorEvents;
    private final SensitiveDataMasker masker;
    private final Map<String, AttemptState> attemptStates = new ConcurrentHashMap<>();

    public RedisToolTraceService(McpGatewayProperties properties,
                                 ObjectProvider<StringRedisTemplate> redisProvider,
                                 ObjectMapper json,
                                 McpMonitorEventService monitorEvents,
                                 SensitiveDataMasker masker) {
        this.properties = properties;
        this.redisProvider = redisProvider;
        this.json = json;
        this.monitorEvents = monitorEvents;
        this.masker = masker;
    }

    /**
     * Agent request has entered the MCP tool gateway.
     */
    public void started(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments) {
        save(context, metadata, arguments, "STARTED", 0, "", 0, 0, 0,
                "Agent request entered MCP", "");
    }

    /**
     * MCP is attempting to call the target Tool server.
     */
    public void attemptStarted(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments,
                               int attempt, int maxAttempts) {
        save(context, metadata, arguments, "ATTEMPTING", 0, "", attempt, maxAttempts, 0,
                "Calling tool server", "");
    }

    /**
     * MCP will retry the Tool call after backoff.
     */
    public void retryWaiting(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments,
                             int failedAttempt, int maxAttempts, long backoffMillis, String failureType) {
        save(context, metadata, arguments, "RETRY_WAITING", 0, failureType, failedAttempt, maxAttempts, backoffMillis,
                "Retry will be attempted after backoff", "");
    }

    /**
     * Tool execution has finished.
     */
    public void finished(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments,
                         long elapsedMillis, boolean success, String failureType, String responseText) {
        save(context, metadata, arguments, success ? "SUCCESS" : "FAILED", elapsedMillis, failureType, 0, 0, 0,
                success ? "Tool call completed" : "Tool call failed", responseText);
    }

    /**
     * Recent live trace entries. One current entry is kept per request id.
     */
    public List<String> recent() {
        if (!properties.redisTraceEnabled()) {
            return List.of("Redis trace is disabled. Set MCP_REDIS_TRACE_ENABLED=true.");
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return List.of("RedisTemplate is not available.");
        }
        try {
            List<String> requestIds = redis.opsForList().range(RECENT_KEY, 0, 49);
            if (requestIds == null || requestIds.isEmpty()) {
                return List.of();
            }
            return requestIds.stream()
                    .map(requestId -> redis.opsForValue().get(KEY_PREFIX + requestId))
                    .filter(value -> value != null && !value.isBlank())
                    .toList();
        } catch (Exception error) {
            log.warn("Redis trace read failed. message={}", error.getMessage());
            return List.of("Redis trace read failed: " + error.getMessage());
        }
    }

    /**
     * Historical accumulation is intentionally disabled.
     *
     * The MCP monitor is used to observe the current Agent request flow only,
     * so this method returns the same live entries as recent().
     */
    public List<String> history() {
        return recent();
    }

    private void save(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments, String status,
                      long elapsedMillis, String failureType, int attempt, int maxAttempts,
                      long backoffMillis, String message, String responseText) {
        String stateKey = stateKey(context, metadata);
        if (attempt > 0 && maxAttempts > 0) {
            attemptStates.put(stateKey, new AttemptState(attempt, maxAttempts));
        }
        String payload;
        try {
            payload = tracePayload(context, metadata, arguments, status, elapsedMillis, failureType, attempt,
                    maxAttempts, backoffMillis, message, responseText);
            monitorEvents.publish(payload);
            if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
                attemptStates.remove(stateKey);
            }
        } catch (Exception error) {
            log.warn("MCP monitor event publish failed. requestId={} tool={} message={}",
                    context.requestId(), metadata.getName(), error.getMessage());
            return;
        }

        if (!properties.redisTraceEnabled()) {
            return;
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            String requestId = context.requestId();
            Duration ttl = Duration.ofSeconds(properties.redisTraceTtlSeconds());
            redis.opsForValue().set(KEY_PREFIX + requestId, payload, ttl);
            redis.opsForList().remove(RECENT_KEY, 0, requestId);
            redis.opsForList().leftPush(RECENT_KEY, requestId);
            redis.opsForList().trim(RECENT_KEY, 0, 49);
            redis.expire(RECENT_KEY, ttl);
        } catch (Exception error) {
            log.warn("Redis trace save failed. requestId={} tool={} message={}",
                    context.requestId(), metadata.getName(), error.getMessage());
        }
    }

    String tracePayload(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments, String status,
                        long elapsedMillis, String failureType, int attempt, int maxAttempts,
                        long backoffMillis, String message, String responseText) throws Exception {
        AttemptState state = attemptStates.get(stateKey(context, metadata));
        int displayAttempt = attempt > 0 ? attempt : state == null ? 0 : state.attempt();
        int displayMaxAttempts = maxAttempts > 0 ? maxAttempts : state == null ? 0 : state.maxAttempts();
        int retryCount = Math.max(0, displayAttempt - 1);
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("requestId", context.requestId());
        trace.put("traceGroupId", context.traceGroupId());
        trace.put("agentId", context.agentId());
        trace.put("userId", context.userId());
        trace.put("clientAddress", context.clientAddress());
        trace.put("toolName", metadata.getName());
        trace.put("operationType", metadata.getOperationType() != null ? metadata.getOperationType().name() : "READ");
        trace.put("status", status);
        trace.put("message", message == null ? "" : message);
        trace.put("failureType", failureType == null ? "" : failureType);
        trace.put("attempt", displayAttempt);
        trace.put("maxAttempts", displayMaxAttempts);
        trace.put("retryCount", retryCount);
        trace.put("backoffMillis", backoffMillis);
        trace.put("elapsedMillis", elapsedMillis);
        trace.put("idempotencyKeyHash", hashText(arguments.path("idempotencyKey").asText("")));
        
        List<String> argNames = new ArrayList<>();
        arguments.fieldNames().forEachRemaining(argNames::add);
        trace.put("argumentNames", argNames);
        
        trace.put("arguments", masker.mask(arguments).toString());
        trace.put("responseSummary", responseSummary(responseText));
        trace.put("timestamp", Instant.now().toString());
        return json.writeValueAsString(trace);
    }

    /**
     * Redis Trace에는 Tool 응답 원문을 저장하지 않고 size/hash/상태/카운트만 저장합니다.
     */
    Map<String, Object> responseSummary(String responseText) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (responseText == null || responseText.isBlank()) {
            summary.put("bytes", 0);
            summary.put("sha256", "");
            return summary;
        }
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        summary.put("bytes", bytes.length);
        summary.put("sha256", sha256(bytes));
        try {
            JsonNode root = json.readTree(responseText);
            summary.put("success", root.path("success").asBoolean(false));
            summary.put("status", root.path("status").asText(""));
            summary.put("toolName", root.path("toolName").asText(""));
            summary.put("pageSize", root.path("pageSize").asInt(0));
            summary.put("pageCount", root.path("pageCount").asInt(0));
            summary.put("returnedCount", root.path("returnedCount").asInt(0));
            summary.put("totalCount", root.path("totalCount").asLong(0));
            summary.put("hasMore", root.path("hasMore").asBoolean(false));
            summary.put("nextCursor", root.path("nextCursor").asText(""));
            summary.put("message", root.path("message").asText(""));
            summary.put("resultRef", root.path("resultRef").asText(""));
        } catch (Exception ignored) {
            summary.put("parseable", false);
        }
        return summary;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception error) {
            return "";
        }
    }

    private String hashText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private String stateKey(McpRequestContext context, ToolMetadata metadata) {
        return context.requestId() + ":" + metadata.getName();
    }

    private record AttemptState(int attempt, int maxAttempts) {
    }
}
