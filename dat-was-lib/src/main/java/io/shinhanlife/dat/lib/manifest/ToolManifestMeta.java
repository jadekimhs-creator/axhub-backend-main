package io.shinhanlife.dat.lib.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Operational metadata exposed by the Tool Service manifest. */
public record ToolManifestMeta(
        String version,
        long timeoutMillis,
        boolean enabled,
        List<String> exampleQueries,
        List<String> tags,
        String legacyInterfaceId,
        List<String> requiredEnvKeys,
        String ownerOrg,
        @JsonProperty("when_to_use") String whenToUse,
        @JsonProperty("when_not_to_use") String whenNotToUse,
        @JsonProperty("io_limits") String ioLimits) {
}
