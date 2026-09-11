package io.shinhanlife.dat.lib.validation;

import java.nio.file.Path;

/** Gradle entry point for validating unique MCP Tool names before packaging. */
public final class McpToolNameValidationRunner {

    private McpToolNameValidationRunner() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: McpToolNameValidationRunner <project-root>");
        }
        validate(Path.of(args[0]));
    }

    static void validate(Path projectRoot) {
        McpToolNameValidator.assertUnique(projectRoot);
    }
}