package io.shinhanlife.dat.lib.manifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dat.lib.config.McpProperties;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import io.shinhanlife.dat.lib.mcp.ToolRegistryHeartbeatSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Builds the Tool Service owned manifest consumed by the MCP server. */
@Service
public class ToolManifestService {

    private static final long DEFAULT_TIMEOUT_MILLIS = 300000L;
    private static final AtomicLong LAST_ISSUED_REVISION = new AtomicLong();
    private final Supplier<List<ToolMetadata>> toolSupplier;
    private final ObjectMapper objectMapper;
    private final McpProperties properties;
    private String lastFingerprint;
    private String lastRevision;

    @Autowired
    public ToolManifestService(ToolRegistryHeartbeatSender heartbeatSender, ObjectMapper objectMapper,
                               McpProperties properties) {
        this(heartbeatSender::getAllScannedTools, objectMapper, properties);
    }

    ToolManifestService(Supplier<List<ToolMetadata>> toolSupplier, ObjectMapper objectMapper,
                        McpProperties properties) {
        this.toolSupplier = toolSupplier;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public ToolManifestResponse currentManifest() {
        return currentManifest(null);
    }

    public ToolManifestResponse currentManifest(String categoryKey) {
        String bundleId = properties.getManifest() == null ? null : properties.getManifest().getBundleId();
        if (bundleId == null || bundleId.isBlank()) {
            throw new IllegalStateException("mcp.manifest.bundle-id must be configured");
        }

        List<ToolManifestItem> tools = toolSupplier.get().stream()
                .filter(tool -> isMatchCategory(categoryKey, tool.getCategoryKey(), bundleId))
                .map(this::toManifestItem)
                .sorted(Comparator.comparing(ToolManifestItem::name))
                .toList();
        validate(tools);
        return new ToolManifestResponse(bundleId, revision(bundleId, tools), tools);
    }

    private boolean isMatchCategory(String requestedCategory, String toolCategory, String bundleId) {
        if (requestedCategory == null) {
            return true;
        }
        if (requestedCategory.equalsIgnoreCase(toolCategory)) {
            return true;
        }
        
        // Return all tools in this module if the requested category matches the module's bundle ID
        if (bundleId != null && 
           (bundleId.equalsIgnoreCase(requestedCategory) || 
            bundleId.equalsIgnoreCase("was-" + requestedCategory))) {
            return true;
        }
        
        return false;
    }

    private ToolManifestItem toManifestItem(ToolMetadata tool) {
        String title = tool.getDisplayName() == null || tool.getDisplayName().isBlank()
                ? tool.getName() : tool.getDisplayName();
        Map<String, Object> schema = tool.getParametersSchema() == null ? emptySchema() : tool.getParametersSchema();
        return new ToolManifestItem(
                tool.getName(), endpoint(tool), title, tool.getDescription(), schema, tool.getOutputSchema(),
                new ToolManifestAnnotations(title, isTrue(tool.getReadOnlyHint()), isTrue(tool.getDestructiveHint()),
                        isTrue(tool.getIdempotentHint()), isTrue(tool.getOpenWorldHint())),
                new ToolManifestMeta(defaultString(tool.getSemver(), "1.0.0"),
                        tool.getTimeoutMillis() == null ? DEFAULT_TIMEOUT_MILLIS : tool.getTimeoutMillis(),
                        tool.getEnabled() == null || tool.getEnabled(),
                        defaultList(tool.getExampleQueries()), defaultList(tool.getTags()),
                        tool.getMciServiceId(), defaultList(tool.getRequiredEnvKeys()), tool.getOwnerOrg(),
                        tool.getWhenToUse(), tool.getWhenNotToUse(), tool.getIoLimits()));
    }

    private void validate(List<ToolManifestItem> tools) {
        String namePrefix = properties.getManifest() == null ? null : properties.getManifest().getNamePrefix();
        Set<String> names = new LinkedHashSet<>();
        for (ToolManifestItem tool : tools) {
            if (tool.name() == null || tool.name().isBlank()) {
                throw new IllegalStateException("Tool manifest contains a blank tool name");
            }
            if (!names.add(tool.name())) {
                throw new IllegalStateException("Tool manifest contains duplicate tool name: " + tool.name());
            }
            if (namePrefix != null && !namePrefix.isBlank() && !tool.name().startsWith(namePrefix)) {
                throw new IllegalStateException("Tool name does not match mcp.manifest.name-prefix: " + tool.name());
            }
            if (!"object".equals(tool.inputSchema().get("type"))) {
                throw new IllegalStateException("Tool inputSchema root type must be object: " + tool.name());
            }
        }
    }

    private synchronized String revision(String bundleId, List<ToolManifestItem> tools) {
        String fingerprint = fingerprint(bundleId, tools);
        if (!fingerprint.equals(lastFingerprint)) {
            long localMinimum = lastRevision == null ? Long.MIN_VALUE : Long.parseLong(lastRevision) + 1;
            long nextTimestamp = LAST_ISSUED_REVISION.updateAndGet(previous ->
                    Math.max(Math.max(System.currentTimeMillis(), localMinimum), previous + 1));
            lastFingerprint = fingerprint;
            lastRevision = Long.toString(nextTimestamp);
        }
        return lastRevision;
    }

    private String fingerprint(String bundleId, List<ToolManifestItem> tools) {
        try {
            return objectMapper.writeValueAsString(Map.of("bundleId", bundleId, "tools", tools));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build Tool manifest revision source", exception);
        }
    }

    private String endpoint(ToolMetadata tool) {
        if (tool.getEndpoint() != null && !tool.getEndpoint().isBlank()) {
            return tool.getEndpoint();
        }
        if (tool.getPodUrl() == null || tool.getPodUrl().isBlank()) {
            return "/mcp/" + tool.getName();
        }
        return tool.getPodUrl().replaceAll("/+$", "") + "/mcp/" + tool.getName();
    }
    private Map<String, Object> emptySchema() {
        return Map.of("type", "object", "properties", Map.of(), "additionalProperties", false);
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<String> defaultList(List<String> value) {
        return value == null ? List.of() : List.copyOf(value);
    }
}
