package io.shinhanlife.dat.lib.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolScaffolderV17SchemaTest {

    @TempDir
    Path root;

    @Test
    void generatesReadOnlyV17MetadataAndCanonicalPagingFields() throws Exception {
        String moduleName = root.resolve("dat-was-v17").toString();
        ToolScaffolder.ToolDefinitionOptions options = new ToolScaffolder.ToolDefinitionOptions(
                null, null, null, null, null,
                List.of("직원 정보를 보여줘"), List.of("employee", "search"), "MCP_TOOL");

        ToolScaffolder.scaffold("search employee", "HR_EMPLOYEE_SEARCH", "직원 조회", "직원을 조회한다.",
                "smp", "HTTP", moduleName, "tester", "2026.09.21", true, null, null, null,
                List.of(new ToolScaffolder.FieldDefinition("ScrPageInfo", "String", "", List.of(), "", true)),
                List.of(), "employee", options);

        Path useCase = Files.walk(root.resolve("dat-was-v17"))
                .filter(path -> path.getFileName().toString().endsWith("UseCase.java"))
                .findFirst()
                .orElseThrow();
        String source = Files.readString(useCase);

        assertTrue(source.contains("requiresApproval = false"), source);
        assertTrue(source.contains("destructive = false"), source);
        assertTrue(source.contains("idempotent = true"), source);
        assertTrue(source.contains("tags = {\"smp\", \"employee\", \"search\", \"조회\", \"상세\"}"), source);
        assertTrue(source.contains("\"직원 정보를 보여줘\""), source);
        assertTrue(source.contains("\"직원 조회 정보를 보여줘\""), source);
        assertTrue(source.contains("\"직원 조회 확인해줘\""), source);
    }

    @Test
    void keepsApprovalForExistingWriteToolNames() throws Exception {
        String moduleName = root.resolve("dat-was-write").toString();
        ToolScaffolder.scaffold("cancel contract", "CONTRACT_CANCEL", "계약 취소", "계약을 취소한다.",
                "smp", "MCI", moduleName, "tester", "2026.09.21", true, "ONB", null, null,
                List.of(), List.of(), "employee", null);

        Path useCase = Files.walk(root.resolve("dat-was-write"))
                .filter(path -> path.getFileName().toString().endsWith("UseCase.java"))
                .findFirst()
                .orElseThrow();
        String source = Files.readString(useCase);

        assertTrue(source.contains("requiresApproval = true"), source);
        assertTrue(source.contains("destructive = true"), source);
        assertTrue(source.contains("idempotent = false"), source);
    }
}
