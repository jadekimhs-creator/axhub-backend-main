package io.shinhanlife.dat.lib.manifest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Server-level routing metadata loaded from tool-service-manifest.yml.
 * Exposes clean service_id and routing_contract matching the official schema.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolServiceManifestResponse(
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("revision") String revision,
        @JsonProperty("routing_contract") Map<String, Object> routingContract) {

    public ToolServiceManifestResponse(String serviceId, String revision, List<Map<String, Object>> routingFunctions) {
        this(serviceId, revision, extractRoutingContract(routingFunctions));
    }

    public ToolServiceManifestResponse(String serviceId, String revision, Map<String, Object> routingContract,
                                      List<Map<String, Object>> routingFunctions) {
        this(serviceId, revision, routingContract != null ? routingContract : extractRoutingContract(routingFunctions));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractRoutingContract(List<Map<String, Object>> functions) {
        if (functions == null || functions.isEmpty()) {
            return null;
        }
        Object first = functions.getFirst();
        if (first instanceof Map<?, ?> map) {
            Object function = map.get("function");
            if (function instanceof Map<?, ?> funcMap && funcMap.containsKey("routing_contract")) {
                return (Map<String, Object>) funcMap.get("routing_contract");
            }
            if (map.containsKey("routing_contract")) {
                return (Map<String, Object>) map.get("routing_contract");
            }
        }
        return null;
    }
}
