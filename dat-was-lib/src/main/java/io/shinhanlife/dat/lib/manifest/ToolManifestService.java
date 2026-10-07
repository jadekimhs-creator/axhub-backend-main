package io.shinhanlife.dat.lib.manifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dat.lib.config.McpProperties;
import io.shinhanlife.dat.lib.config.ToolServiceProperties;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import io.shinhanlife.dat.lib.mcp.ToolMetadataMcpMapper;
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

    private static final long DEFAULT_TIMEOUT_MILLIS = 5000L;
    private static final int DEFAULT_RETRY_MAX_ATTEMPTS = 3;
    private static final AtomicLong LAST_ISSUED_REVISION = new AtomicLong();
    private final Supplier<List<ToolMetadata>> toolSupplier;
    private final ObjectMapper objectMapper;
    private final McpProperties properties;
    private final ToolServiceProperties toolServiceProperties;
    private String lastFingerprint;
    private String lastRevision;

    @Autowired
    public ToolManifestService(ToolRegistryHeartbeatSender heartbeatSender, ObjectMapper objectMapper,
                               McpProperties properties,
                               @Autowired(required = false) ToolServiceProperties toolServiceProperties) {
        this(heartbeatSender::getAllScannedTools, objectMapper, properties, toolServiceProperties);
    }

    ToolManifestService(Supplier<List<ToolMetadata>> toolSupplier, ObjectMapper objectMapper,
                        McpProperties properties) {
        this(toolSupplier, objectMapper, properties, null);
    }

    ToolManifestService(Supplier<List<ToolMetadata>> toolSupplier, ObjectMapper objectMapper,
                        McpProperties properties, ToolServiceProperties toolServiceProperties) {
        this.toolSupplier = toolSupplier;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.toolServiceProperties = toolServiceProperties;
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

    public ToolServiceManifestResponse currentToolServiceManifest() {
        McpProperties.Manifest manifest = properties.getManifest();
        String serviceId = manifest == null ? null : manifest.getServiceId();
        if ((serviceId == null || serviceId.isBlank()) && toolServiceProperties != null && toolServiceProperties.hasServiceId()) {
            serviceId = toolServiceProperties.getServiceId();
        }

        McpProperties.RoutingFunction singleContract = null;
        if (manifest != null && manifest.getRoutingContract() != null) {
            singleContract = manifest.getRoutingContract();
        } else if (toolServiceProperties != null && toolServiceProperties.hasRoutingContract()) {
            singleContract = toolServiceProperties.getRoutingContract();
        }

        List<McpProperties.RoutingFunction> configured = manifest == null
                ? List.of() : defaultRoutingList(manifest.getRoutingFunctions());
        if (configured.isEmpty() && toolServiceProperties != null && !toolServiceProperties.getRoutingFunctions().isEmpty()) {
            configured = toolServiceProperties.getRoutingFunctions();
        }

        if (singleContract == null && !configured.isEmpty()) {
            singleContract = configured.getFirst();
        }

        Map<String, Object> routingContractMap = singleContract != null ? toRoutingContractMap(singleContract) : null;

        List<Map<String, Object>> routingFunctions = configured.stream()
                .map(this::toRoutingFunctionEnvelope)
                .toList();

        if (routingContractMap == null && routingFunctions.isEmpty()) {
            Map<String, Object> directYaml = loadDirectYamlManifest();
            if (!directYaml.isEmpty()) {
                if (serviceId == null || serviceId.isBlank()) {
                    Object sid = directYaml.get("service_id");
                    if (sid == null) sid = directYaml.get("service-id");
                    if (sid != null) serviceId = sid.toString();
                }
                Map<?, ?> rc = null;
                if (directYaml.get("routing_contract") instanceof Map<?, ?> map) rc = map;
                else if (directYaml.get("routing-contract") instanceof Map<?, ?> map) rc = map;
                else if (directYaml.get("mcp") instanceof Map<?, ?> mcp && mcp.get("manifest") instanceof Map<?, ?> mf) {
                    if (mf.get("routing_contract") instanceof Map<?, ?> map) rc = map;
                    else if (mf.get("routing-contract") instanceof Map<?, ?> map) rc = map;
                }
                if (rc != null) {
                    routingContractMap = normalizeContractMap(rc);
                    McpProperties.RoutingFunction derived = fromContractMap(routingContractMap, serviceId);
                    routingFunctions = List.of(toRoutingFunctionEnvelope(derived));
                }
            }
        }

        try {
            Map<String, Object> sourceMap = new LinkedHashMap<>();
            sourceMap.put("service_id", serviceId);
            if (routingContractMap != null) {
                sourceMap.put("routing_contract", routingContractMap);
            }
            String source = objectMapper.writeValueAsString(sourceMap);
            return new ToolServiceManifestResponse(serviceId, sha256(source), routingContractMap);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build Tool Service manifest", exception);
        }
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
        Map<String, Object> schema = ToolMetadataMcpMapper.normalizeSchema(tool.getParametersSchema());
        return new ToolManifestItem(
                tool.getName(), endpoint(tool), title, tool.getDescription(), schema, tool.getOutputSchema(),
                new ToolManifestAnnotations(title, isTrue(tool.getReadOnlyHint()), isTrue(tool.getDestructiveHint()),
                        isTrue(tool.getIdempotentHint()), isTrue(tool.getOpenWorldHint())),
                new ToolManifestMeta(defaultString(tool.getSemver(), "1.0.0"),
                        positiveOrDefault(tool.getTimeoutMillis(), DEFAULT_TIMEOUT_MILLIS),
                        positiveOrDefault(tool.getRetryMaxAttempts(), DEFAULT_RETRY_MAX_ATTEMPTS),
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

    private long positiveOrDefault(Long value, long defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
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

    private List<McpProperties.RoutingFunction> defaultRoutingList(List<McpProperties.RoutingFunction> value) {
        return value == null ? List.of() : List.copyOf(value);
    }

    private Map<String, Object> toRoutingContractMap(McpProperties.RoutingFunction route) {
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("business_domain", defaultList(route.getBusinessDomain()));
        contract.put("business_outcome", defaultList(route.getBusinessOutcome()));
        contract.put("primary_entities", defaultList(route.getPrimaryEntities()));
        contract.put("select_if", defaultList(route.getSelectIf()));
        contract.put("reject_if", defaultList(route.getRejectIf()));
        contract.put("capability_index", defaultList(route.getCapabilityIndex()));
        contract.put("confusable_servers", defaultList(route.getConfusableServers()));
        return contract;
    }

    private Map<String, Object> toRoutingFunctionEnvelope(McpProperties.RoutingFunction route) {
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("schema_version", "3.0");
        contract.put("server_id", route.getServerId());
        contract.put("category_key", route.getCategoryKey());
        contract.put("product_boundary", route.getProductBoundary());
        contract.put("business_domain", defaultList(route.getBusinessDomain()));
        contract.put("business_outcome", defaultList(route.getBusinessOutcome()));
        contract.put("primary_entities", defaultList(route.getPrimaryEntities()));
        contract.put("select_if", defaultList(route.getSelectIf()));
        contract.put("reject_if", defaultList(route.getRejectIf()));
        contract.put("capability_index", defaultList(route.getCapabilityIndex()));
        contract.put("confusable_servers", defaultList(route.getConfusableServers()));
        contract.put("decision_policy", route.getDecisionPolicy());

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", route.getName() != null && !route.getName().isBlank() ? route.getName() : "route_to_" + route.getServerId());
        function.put("description_serialization", routingDescription(route, contract));
        function.put("routing_contract", contract);
        function.put("parameters", emptySchema());

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", "function");
        envelope.put("function", function);
        return envelope;
    }

    private String routingDescription(McpProperties.RoutingFunction route, Map<String, Object> contract) {
        try {
            String outcome = String.join(" ", defaultList(route.getBusinessOutcome()));
            String policy = nonBlank(route.getDecisionPolicy());
            String defaultDesc = (outcome + " " + policy).trim();
            return defaultString(route.getDescriptionSerialization(), defaultDesc);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize routing contract", exception);
        }
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte item : digest) result.append(String.format("%02x", item));
        return result.toString();
    }

    private String nonBlank(String value) {
        return value == null ? "" : value.trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadDirectYamlManifest() {
        try {
            org.springframework.core.io.ClassPathResource resource =
                    new org.springframework.core.io.ClassPathResource("tool-service-manifest.yml");
            if (resource.exists()) {
                try (java.io.InputStream in = resource.getInputStream()) {
                    return new com.fasterxml.jackson.dataformat.yaml.YAMLMapper().readValue(in, Map.class);
                }
            }
        } catch (Exception ignored) {
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeContractMap(Map<?, ?> raw) {
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("business_domain", coerceToList(firstNonNull(raw.get("business_domain"), raw.get("business-domain"))));
        contract.put("business_outcome", coerceToList(firstNonNull(raw.get("business_outcome"), raw.get("business-outcome"))));
        contract.put("primary_entities", coerceToList(firstNonNull(raw.get("primary_entities"), raw.get("primary-entities"))));
        contract.put("select_if", coerceToList(firstNonNull(raw.get("select_if"), raw.get("select-if"))));
        contract.put("reject_if", coerceToList(firstNonNull(raw.get("reject_if"), raw.get("reject-if"))));
        contract.put("capability_index", coerceToList(firstNonNull(raw.get("capability_index"), raw.get("capability-index"), raw.get("capabilities"))));
        contract.put("confusable_servers", coerceToList(firstNonNull(raw.get("confusable_servers"), raw.get("confusable-servers"))));
        return contract;
    }

    @SuppressWarnings("unchecked")
    private McpProperties.RoutingFunction fromContractMap(Map<String, Object> contractMap, String serviceId) {
        McpProperties.RoutingFunction fn = new McpProperties.RoutingFunction();
        fn.setServerId(serviceId);
        fn.setServiceId(serviceId);
        fn.setBusinessDomain(contractMap.get("business_domain"));
        fn.setBusinessOutcome(contractMap.get("business_outcome"));
        fn.setPrimaryEntities(contractMap.get("primary_entities"));
        fn.setSelectIf(contractMap.get("select_if"));
        fn.setRejectIf(contractMap.get("reject_if"));
        fn.setCapabilityIndex(contractMap.get("capability_index"));
        fn.setConfusableServers(contractMap.get("confusable_servers"));
        return fn;
    }

    private static Object firstNonNull(Object... values) {
        for (Object v : values) {
            if (v != null) return v;
        }
        return null;
    }

    private static List<String> coerceToList(Object value) {
        if (value == null) return List.of();
        if (value instanceof java.util.Collection<?> coll) {
            List<String> list = new java.util.ArrayList<>();
            for (Object item : coll) {
                if (item != null && !item.toString().isBlank()) {
                    list.add(item.toString().trim());
                }
            }
            return List.copyOf(list);
        }
        String str = value.toString().trim();
        if (str.isBlank()) return List.of();
        return List.of(str);
    }
}
