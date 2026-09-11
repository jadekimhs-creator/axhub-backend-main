package io.shinhanlife.dat.lib.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @package io.shinhanlife.dat.lib.validation
 * @className McpToolNameValidatorTest
 * @description MCP tool name uniqueness validation test
 * @author 0986406
 * @create 2026.07.27
 * <pre>
 * ---------- revision history ----------
 * date       author    description
 * ---------- --------- ---------------------------
 * 2026.07.27 0986406    initial creation
 * </pre>
 */
class McpToolNameValidatorTest {

    @TempDir
    Path temporaryRoot;

    @Test
    void rejectsDuplicateMcpToolNamesAcrossToolModules() throws IOException {
        writeToolSource("dat-was-first", "FirstTool.java", "first", "oth_sms_notification_send");
        writeToolSource("dat-was-second", "SecondTool.java", "second", "oth_sms_notification_send");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> McpToolNameValidator.assertUnique(temporaryRoot));

        assertTrue(exception.getMessage().contains("oth_sms_notification_send"));
        assertTrue(exception.getMessage().contains("dat-was-first"));
        assertTrue(exception.getMessage().contains("dat-was-second"));
    }

    @Test
    void validationRunnerRejectsDuplicateMcpToolNamesBeforePackaging() throws IOException {
        writeToolSource("dat-was-first", "FirstTool.java", "first", "oth_sms_notification_send");
        writeToolSource("dat-was-second", "SecondTool.java", "second", "oth_sms_notification_send");

        assertThrows(IllegalStateException.class,
                () -> McpToolNameValidationRunner.validate(temporaryRoot));
    }

    @Test
    void rejectsToolNameOutsideConfiguredPattern() throws IOException {
        writeToolSource("dat-was-first", "FirstTool.java", "first", "bond.issue");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> McpToolNameValidator.assertUnique(temporaryRoot));

        assertTrue(exception.getMessage().contains("Invalid MCP tool name(s)"));
        assertTrue(exception.getMessage().contains("bond.issue"));
    }
    @Test
    void acceptsLettersDigitsUnderscoresAndDashesWithin128Characters() throws IOException {
        String validName = "Tool_Name-" + "a".repeat(118);
        writeToolSource("dat-was-first", "FirstTool.java", "first", validName);

        assertDoesNotThrow(() -> McpToolNameValidator.assertUnique(temporaryRoot));
    }

    @Test
    void rejectsToolNameLongerThan128Characters() throws IOException {
        String invalidName = "a".repeat(129);
        writeToolSource("dat-was-first", "FirstTool.java", "first", invalidName);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> McpToolNameValidator.assertUnique(temporaryRoot));

        assertTrue(exception.getMessage().contains(invalidName));
    }
    @Test
    void acceptsCurrentProjectToolNames() {
        assertDoesNotThrow(() -> McpToolNameValidator.assertUnique(findProjectRoot()));
    }

    private void writeToolSource(String moduleName, String fileName, String className, String toolName) throws IOException {
        Path source = temporaryRoot.resolve(moduleName).resolve("src/main/java/example").resolve(fileName);
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package example;

                import org.springaicommunity.mcp.annotation.McpTool;

                class %s {
                    @McpTool(name = "%s")
                    void execute() { }
                }
                """.formatted(className, toolName));
    }

    private Path findProjectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Could not locate Gradle project root");
        }
        return current;
    }
}
