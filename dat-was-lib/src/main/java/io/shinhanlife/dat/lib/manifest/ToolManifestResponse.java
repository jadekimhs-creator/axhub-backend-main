package io.shinhanlife.dat.lib.manifest;

import java.util.List;

/** Top-level response for GET /tool-manifest. */
public record ToolManifestResponse(String bundleId, String revision, List<ToolManifestItem> tools) {
}