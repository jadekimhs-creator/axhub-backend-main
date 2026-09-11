package io.shinhanlife.dat.lib.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToolSourceUpdaterTest {

    @TempDir
    Path temporaryRoot;

    @Test
    void updatesSdkAndProjectOwnedMetadataOnTheSameToolMethod() throws Exception {
        Path source = temporaryRoot.resolve("dat-was-cus/src/main/java/example/SampleUseCase.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package example;
                import org.springaicommunity.mcp.annotation.McpTool;
                import io.shinhanlife.dat.lib.annotation.GrowToolHint;
                interface SampleUseCase {
                    @McpTool(name = "cmm_sample_search", description = "old")
                    @GrowToolHint(requiresApproval = false)
                    void search();
                }
                """);

        ToolSourceUpdater.updateToolSource(temporaryRoot, "cmm_sample_search", "customer", "new", true, true);

        String updated = Files.readString(source);
        assertTrue(updated.contains("@McpTool(name = \"cmm_sample_search\", description = \"new\")"));
        assertTrue(updated.contains("register = true"), updated);
        assertTrue(updated.contains("requiresApproval = true"), updated);
        assertTrue(updated.contains("categoryKey = \"customer\""), updated);
    }
}
