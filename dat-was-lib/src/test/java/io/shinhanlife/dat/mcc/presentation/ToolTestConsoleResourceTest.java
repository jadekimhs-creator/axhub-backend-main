package io.shinhanlife.dat.mcc.presentation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ToolTestConsoleResourceTest {

    @Test
    void publishesManifestDrivenToolTestConsole() throws Exception {
        try (InputStream resource = getClass().getResourceAsStream("/static/tool-test-console.html")) {
            assertNotNull(resource);
            String html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(html.contains("/tool-manifest"));
            assertTrue(html.contains("Run saved cases"));
            assertTrue(html.contains("localStorage"));
            assertTrue(html.contains("request-id"));
        }
    }
}