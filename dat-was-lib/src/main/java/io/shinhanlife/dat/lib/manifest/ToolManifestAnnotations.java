package io.shinhanlife.dat.lib.manifest;

/** Behaviour hints exposed by the Tool Service manifest. */
public record ToolManifestAnnotations(
        String title,
        boolean readOnlyHint,
        boolean destructiveHint,
        boolean idempotentHint,
        boolean openWorldHint) {
}