package io.shinhanlife.dat.mcc.presentation;

import io.shinhanlife.dat.lib.manifest.ToolManifestResponse;
import io.shinhanlife.dat.lib.manifest.ToolManifestService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

/** Read-only Tool Service manifest endpoint for MCP background discovery. */
@RestController
public class ToolManifestController {

    private final ToolManifestService toolManifestService;

    public ToolManifestController(ToolManifestService toolManifestService) {
        this.toolManifestService = toolManifestService;
    }

    @GetMapping(value = "/tool-manifest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ToolManifestResponse> getManifest(
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        return handleManifest(toolManifestService.currentManifest(), ifNoneMatch);
    }

    @GetMapping(value = "/tool-manifest/{categoryKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ToolManifestResponse> getManifestByCategory(
            @PathVariable("categoryKey") String categoryKey,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        return handleManifest(toolManifestService.currentManifest(categoryKey), ifNoneMatch);
    }

    private ResponseEntity<ToolManifestResponse> handleManifest(ToolManifestResponse manifest, String ifNoneMatch) {
        String eTag = '"' + manifest.revision() + '"';
        if (eTag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).eTag(eTag).build();
        }
        return ResponseEntity.ok().eTag(eTag).body(manifest);
    }
}