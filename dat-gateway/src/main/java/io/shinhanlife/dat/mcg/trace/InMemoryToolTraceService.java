package io.shinhanlife.dat.mcg.trace;

import io.shinhanlife.dat.mcg.config.McpGatewayProperties;
import io.shinhanlife.dat.mcg.security.McpRequestContext;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @package io.shinhanlife.dat.mcg.trace
 * @className InMemoryToolTraceService
 * @description AX HUB 시스템 인메모리 툴 트레이스 서비스
 * @author 0986406
 * @create 2026.09.01
 */
@Service
public class InMemoryToolTraceService {
    private static final Logger log = LoggerFactory.getLogger(InMemoryToolTraceService.class);

    private final McpGatewayProperties properties;
    private final ObjectMapper json;
    private final McpMonitorEventService monitorEvents;
    private final Map<String, AttemptState> attemptStates = new ConcurrentHashMap<>();
    
    // LRU Cache for recent traces
    private final Map<String, String> recentTraces;

    public InMemoryToolTraceService(McpGatewayProperties properties,
                                 ObjectMapper json,
                                 McpMonitorEventService monitorEvents) {
        this.properties = properties;
        this.json = json;
        this.monitorEvents = monitorEvents;
        
        final int maxSize = (int) properties.traceRetentionSize();
        this.recentTraces = new LinkedHashMap<String, String>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > maxSize;
            }
        };
    }

    public void started(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments) {
        save(context, metadata, arguments, "STARTED", 0, "", 0, 0, 0,
                "Agent request entered MCP", "");
    }

    public void attemptStarted(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments,
                               int attempt, int maxAttempts) {
        save(context, metadata, arguments, "ATTEMPTING", 0, "", attempt, maxAttempts, 0,
                "Calling tool server", "");
    }

    public void retryWaiting(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments,
                             int failedAttempt, int maxAttempts, long backoffMillis, String failureType) {
        save(context, metadata, arguments, "RETRY_WAITING", 0, failureType, failedAttempt, maxAttempts, backoffMillis,
                "Retry will be attempted after backoff", "");
    }

    public void finished(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments,
                         long elapsedMillis, boolean success, String failureType, String responseText) {
        save(context, metadata, arguments, success ? "SUCCESS" : "FAILED", elapsedMillis, failureType, 0, 0, 0,
                success ? "Tool call completed" : "Tool call failed", responseText);
    }

    public List<String> getRecentTraces(int limit) {
        if (!properties.traceEnabled()) {
            return List.of("Trace is disabled. Set traceEnabled=true.");
        }
        
        List<String> result = new ArrayList<>();
        synchronized (recentTraces) {
            List<String> values = new ArrayList<>(recentTraces.values());
            // reverse to get latest first
            for (int i = values.size() - 1; i >= 0 && result.size() < limit; i--) {
                result.add(values.get(i));
            }
        }
        return result;
    }

    public String getTraceDetail(String requestId) {
        if (!properties.traceEnabled()) {
            return "Trace is disabled.";
        }
        synchronized (recentTraces) {
            return recentTraces.get(requestId);
        }
    }

    private void save(McpRequestContext context, ToolMetadata metadata, ObjectNode arguments,
                      String state, long elapsedMillis, String failureType, int attempt, int maxAttempts, long backoffMillis,
                      String message, String responseText) {
        
        if (!properties.traceEnabled()) {
            return; // trace is off
        }

        try {
            AttemptState ast = attemptStates.computeIfAbsent(context.requestId(), k -> new AttemptState(0));
            if (attempt > 0) ast.currentAttempt = attempt;
            
            ObjectNode root = json.createObjectNode();
            root.put("requestId", context.requestId());
            root.put("agentId", context.agentId());
            root.put("tenantId", context.traceGroupId());
            root.put("toolName", metadata.getName());
            root.put("targetPod", metadata.getPodUrl());
            root.put("state", state);
            root.put("timestamp", Instant.now().toString());
            root.put("elapsedMillis", elapsedMillis);
            root.put("attempt", ast.currentAttempt);
            root.put("maxAttempts", maxAttempts > 0 ? maxAttempts : properties.retryMaxAttempts());
            root.put("backoffMillis", backoffMillis);
            
            if (failureType != null && !failureType.isEmpty()) {
                root.put("failureType", failureType);
            }
            if (message != null && !message.isEmpty()) {
                root.put("message", message);
            }
            
            // Mask arguments if needed, for simplicity we just put them
            root.set("arguments", arguments);

            if (responseText != null && !responseText.isEmpty()) {
                if (responseText.length() > 500) {
                    root.put("responsePreview", responseText.substring(0, 500) + "...");
                } else {
                    root.put("responsePreview", responseText);
                }
            }

            String payload = json.writeValueAsString(root);
            
            synchronized (recentTraces) {
                recentTraces.put(context.requestId(), payload);
            }
            
            monitorEvents.publish(payload);
            
            if ("SUCCESS".equals(state) || "FAILED".equals(state)) {
                attemptStates.remove(context.requestId());
            }

        } catch (Exception e) {
            log.error("Failed to save trace: {}", e.getMessage());
        }
    }

    private static class AttemptState {
        int currentAttempt;
        AttemptState(int currentAttempt) {
            this.currentAttempt = currentAttempt;
        }
    }
}
