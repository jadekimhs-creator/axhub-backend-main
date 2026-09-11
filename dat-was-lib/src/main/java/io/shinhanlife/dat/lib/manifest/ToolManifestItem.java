package io.shinhanlife.dat.lib.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** One MCP Tool declaration published by a Tool Service. */
public record ToolManifestItem(
        String name,
        String endpoint,
        String title,
        String description,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        ToolManifestAnnotations annotations,
        @JsonProperty("_meta") ToolManifestMeta meta) {
}
