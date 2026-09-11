package io.shinhanlife.dat.mcg.document;

import io.shinhanlife.dat.mcc.dto.ToolMetadata;

public record DocumentGenerationRequest(
        ToolMetadata tool,
        String version,
        boolean includeProgram,
        boolean includeProcess,
        boolean includeRevision) {
}
