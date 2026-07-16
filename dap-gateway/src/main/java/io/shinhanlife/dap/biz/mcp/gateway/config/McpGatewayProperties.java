package io.shinhanlife.dap.biz.mcp.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "mcp")
/**
 * application.properties의 mcp.* 설정을 Java 코드에서 사용하기 위한 설정 클래스입니다.
 *
 * API Key, 허용 Tool, 감사 로그, Redis Trace, Retry, Circuit Breaker,
 * EIMS/Tool 서버 주소 같은 MCP Gateway 운영 설정을 한 곳에서 관리합니다.
 */
public class McpGatewayProperties {
    private String apiKey;
    private List<String> allowedTools;
    private Boolean auditEnabled;
    private Boolean agentClaimsRequired;
    private Boolean trustedClaimsRequired;
    private Boolean writeApprovalRequired;
    private Boolean redisTraceEnabled;
    private Long redisTraceTtlSeconds;
    private Long toolTimeoutMillis;
    private Integer retryMaxAttempts;
    private Long retryInitialBackoffMillis;
    private Double retryBackoffMultiplier;
    private Long retryMaxBackoffMillis;
    private Integer circuitBreakerFailureThreshold;
    private Long circuitBreakerOpenMillis;
    private String eimsBaseUrl;
    private Long eimsTimeoutMillis;
    private String toolServerBaseUrl;
    private String toolServerApiKey;
    private Long toolHeartbeatTimeoutSeconds;
    private Integer defaultPageSize;
    private Integer maxPageSize;
    private Integer maxPagesPerCall;
    private Long maxStreamBytes;
    private Long maxDurationSeconds;
    private Long maxResponseBytesFromTool;
    private Long largeResponseMaxItemBytes;
    private Integer largeResponseMaxPreviewItems;
    private Integer toolExecutorCorePoolSize;
    private Integer toolExecutorMaxPoolSize;
    private Integer toolExecutorQueueCapacity;
    private Long toolExecutorKeepAliveSeconds;

    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setAllowedTools(List<String> allowedTools) { this.allowedTools = allowedTools; }
    public void setAuditEnabled(Boolean auditEnabled) { this.auditEnabled = auditEnabled; }
    public void setAgentClaimsRequired(Boolean agentClaimsRequired) { this.agentClaimsRequired = agentClaimsRequired; }
    public void setTrustedClaimsRequired(Boolean trustedClaimsRequired) { this.trustedClaimsRequired = trustedClaimsRequired; }
    public void setWriteApprovalRequired(Boolean writeApprovalRequired) { this.writeApprovalRequired = writeApprovalRequired; }
    public void setRedisTraceEnabled(Boolean redisTraceEnabled) { this.redisTraceEnabled = redisTraceEnabled; }
    public void setRedisTraceTtlSeconds(Long redisTraceTtlSeconds) { this.redisTraceTtlSeconds = redisTraceTtlSeconds; }
    public void setToolTimeoutMillis(Long toolTimeoutMillis) { this.toolTimeoutMillis = toolTimeoutMillis; }
    public void setRetryMaxAttempts(Integer retryMaxAttempts) { this.retryMaxAttempts = retryMaxAttempts; }
    public void setRetryInitialBackoffMillis(Long retryInitialBackoffMillis) { this.retryInitialBackoffMillis = retryInitialBackoffMillis; }
    public void setRetryBackoffMultiplier(Double retryBackoffMultiplier) { this.retryBackoffMultiplier = retryBackoffMultiplier; }
    public void setRetryMaxBackoffMillis(Long retryMaxBackoffMillis) { this.retryMaxBackoffMillis = retryMaxBackoffMillis; }
    public void setCircuitBreakerFailureThreshold(Integer circuitBreakerFailureThreshold) { this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold; }
    public void setCircuitBreakerOpenMillis(Long circuitBreakerOpenMillis) { this.circuitBreakerOpenMillis = circuitBreakerOpenMillis; }
    public void setEimsBaseUrl(String eimsBaseUrl) { this.eimsBaseUrl = eimsBaseUrl; }
    public void setEimsTimeoutMillis(Long eimsTimeoutMillis) { this.eimsTimeoutMillis = eimsTimeoutMillis; }
    public void setToolServerBaseUrl(String toolServerBaseUrl) { this.toolServerBaseUrl = toolServerBaseUrl; }
    public void setToolServerApiKey(String toolServerApiKey) { this.toolServerApiKey = toolServerApiKey; }
    public void setToolHeartbeatTimeoutSeconds(Long toolHeartbeatTimeoutSeconds) { this.toolHeartbeatTimeoutSeconds = toolHeartbeatTimeoutSeconds; }
    public void setDefaultPageSize(Integer defaultPageSize) { this.defaultPageSize = defaultPageSize; }
    public void setMaxPageSize(Integer maxPageSize) { this.maxPageSize = maxPageSize; }
    public void setMaxPagesPerCall(Integer maxPagesPerCall) { this.maxPagesPerCall = maxPagesPerCall; }
    public void setMaxStreamBytes(Long maxStreamBytes) { this.maxStreamBytes = maxStreamBytes; }
    public void setMaxDurationSeconds(Long maxDurationSeconds) { this.maxDurationSeconds = maxDurationSeconds; }
    public void setMaxResponseBytesFromTool(Long maxResponseBytesFromTool) { this.maxResponseBytesFromTool = maxResponseBytesFromTool; }
    public void setLargeResponseMaxItemBytes(Long largeResponseMaxItemBytes) { this.largeResponseMaxItemBytes = largeResponseMaxItemBytes; }
    public void setLargeResponseMaxPreviewItems(Integer largeResponseMaxPreviewItems) { this.largeResponseMaxPreviewItems = largeResponseMaxPreviewItems; }
    public void setToolExecutorCorePoolSize(Integer toolExecutorCorePoolSize) { this.toolExecutorCorePoolSize = toolExecutorCorePoolSize; }
    public void setToolExecutorMaxPoolSize(Integer toolExecutorMaxPoolSize) { this.toolExecutorMaxPoolSize = toolExecutorMaxPoolSize; }
    public void setToolExecutorQueueCapacity(Integer toolExecutorQueueCapacity) { this.toolExecutorQueueCapacity = toolExecutorQueueCapacity; }
    public void setToolExecutorKeepAliveSeconds(Long toolExecutorKeepAliveSeconds) { this.toolExecutorKeepAliveSeconds = toolExecutorKeepAliveSeconds; }

    public String apiKey() { return apiKey; }
    public boolean apiKeyEnabled() { return apiKey != null && !apiKey.isBlank(); }
    public boolean auditEnabled() { return auditEnabled == null || auditEnabled; }
    public boolean agentClaimsRequired() { return agentClaimsRequired == null || agentClaimsRequired; }
    public boolean trustedClaimsRequired() { return trustedClaimsRequired == null || trustedClaimsRequired; }
    public boolean writeApprovalRequired() { return writeApprovalRequired == null || writeApprovalRequired; }
    public boolean redisTraceEnabled() { return redisTraceEnabled == null || redisTraceEnabled; }
    public long redisTraceTtlSeconds() { return redisTraceTtlSeconds == null || redisTraceTtlSeconds < 1 ? 3_600 : redisTraceTtlSeconds; }
    public long toolTimeoutMillis() { return toolTimeoutMillis == null || toolTimeoutMillis < 1 ? 5_000 : toolTimeoutMillis; }
    public int retryMaxAttempts() { return retryMaxAttempts == null || retryMaxAttempts < 1 ? 3 : retryMaxAttempts; }
    public long retryInitialBackoffMillis() { return retryInitialBackoffMillis == null || retryInitialBackoffMillis < 1 ? 1_000 : retryInitialBackoffMillis; }
    public double retryBackoffMultiplier() { return retryBackoffMultiplier == null || retryBackoffMultiplier < 1.0 ? 2.0 : retryBackoffMultiplier; }
    public long retryMaxBackoffMillis() { return retryMaxBackoffMillis == null || retryMaxBackoffMillis < 1 ? 5_000 : retryMaxBackoffMillis; }
    public int circuitBreakerFailureThreshold() { return circuitBreakerFailureThreshold == null || circuitBreakerFailureThreshold < 1 ? 5 : circuitBreakerFailureThreshold; }
    public long circuitBreakerOpenMillis() { return circuitBreakerOpenMillis == null || circuitBreakerOpenMillis < 1 ? 30_000 : circuitBreakerOpenMillis; }
    public String eimsBaseUrl() { return eimsBaseUrl == null ? "" : eimsBaseUrl.trim(); }
    public long eimsTimeoutMillis() { return eimsTimeoutMillis == null || eimsTimeoutMillis < 1 ? 3_000 : eimsTimeoutMillis; }
    public String toolServerBaseUrl() { return toolServerBaseUrl == null || toolServerBaseUrl.isBlank() ? "http://localhost:9090" : toolServerBaseUrl.trim(); }
    public String toolServerApiKey() { return toolServerApiKey == null ? "" : toolServerApiKey.trim(); }
    public long toolHeartbeatTimeoutSeconds() { return toolHeartbeatTimeoutSeconds == null || toolHeartbeatTimeoutSeconds < 1 ? 30 : toolHeartbeatTimeoutSeconds; }
    public int defaultPageSize() { return defaultPageSize == null || defaultPageSize < 1 ? 100 : defaultPageSize; }
    public int maxPageSize() { return maxPageSize == null || maxPageSize < 1 ? 500 : maxPageSize; }
    public int maxPagesPerCall() { return maxPagesPerCall == null || maxPagesPerCall < 1 ? 3 : maxPagesPerCall; }
    public long maxStreamBytes() { return maxStreamBytes == null || maxStreamBytes < 1 ? 1_048_576 : maxStreamBytes; }
    public long maxDurationSeconds() { return maxDurationSeconds == null || maxDurationSeconds < 1 ? 10 : maxDurationSeconds; }
    public long maxResponseBytesFromTool() { return maxResponseBytesFromTool == null || maxResponseBytesFromTool < 1 ? 5_242_880 : maxResponseBytesFromTool; }
    public long largeResponseMaxItemBytes() { return largeResponseMaxItemBytes == null || largeResponseMaxItemBytes < 1 ? 16_384 : largeResponseMaxItemBytes; }
    public int largeResponseMaxPreviewItems() { return largeResponseMaxPreviewItems == null || largeResponseMaxPreviewItems < 1 ? 100 : largeResponseMaxPreviewItems; }
    public int toolExecutorCorePoolSize() { return toolExecutorCorePoolSize == null || toolExecutorCorePoolSize < 1 ? 20 : toolExecutorCorePoolSize; }
    public int toolExecutorMaxPoolSize() {
        int core = toolExecutorCorePoolSize();
        return toolExecutorMaxPoolSize == null || toolExecutorMaxPoolSize < core ? Math.max(core, 100) : toolExecutorMaxPoolSize;
    }
    public int toolExecutorQueueCapacity() { return toolExecutorQueueCapacity == null || toolExecutorQueueCapacity < 1 ? 200 : toolExecutorQueueCapacity; }
    public long toolExecutorKeepAliveSeconds() { return toolExecutorKeepAliveSeconds == null || toolExecutorKeepAliveSeconds < 1 ? 60 : toolExecutorKeepAliveSeconds; }

    public Set<String> allowedToolSet() {
        if (allowedTools == null) {
            return Set.of();
        }
        return allowedTools.stream()
                .filter(tool -> tool != null && !tool.isBlank())
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }
}
