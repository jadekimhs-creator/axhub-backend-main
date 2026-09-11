package io.shinhanlife.dat.report.source;

import static org.assertj.core.api.Assertions.assertThat;

import io.shinhanlife.dat.report.config.ReportProperties;
import io.shinhanlife.dat.report.model.ToolSourceType;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ToolSourceDiscoveryTest {

    private final Path adminRoot = projectRoot();
    private final Path dapmtRoot = adminRoot.resolve("../dap-was-dapmt").normalize();

    @Test
    void discoversToolsFromBothSelectableProjects() {
        ToolSourceDiscovery discovery = discovery();

        assertThat(discovery.discover(ToolSourceType.DAP_ADMIN))
                .extracting(tool -> tool.name())
                .contains("cmm_claim_search");
        assertThat(discovery.discover(ToolSourceType.DAP_WAS_DAPMT))
                .extracting(tool -> tool.name())
                .contains("spr_field_inquiry_list");
    }

    @Test
    void collectsParametersFromDtoAnnotationsInsteadOfYamlDefinitions() {
        ToolSourceDiscovery discovery = discovery();
        var tool = discovery.discover(ToolSourceType.DAP_WAS_DAPMT).stream()
                .filter(candidate -> candidate.name().equals("spr_field_inquiry_list"))
                .findFirst().orElseThrow();

        var report = new ToolDetailAnalyzer(discovery).analyze(tool, ToolSourceType.DAP_WAS_DAPMT);

        assertThat(report.fields()).anySatisfy(field -> {
            assertThat(field.sourceKind()).isEqualTo("DTO");
            assertThat(field.direction()).isEqualTo("INPUT");
            assertThat(field.fieldName()).isEqualTo("inquiryId");
            assertThat(field.constraints()).contains("Schema=");
        });
        assertThat(report.fields()).noneMatch(field -> field.sourceKind().equals("TOOL_DEFINITION"));
    }

    private ToolSourceDiscovery discovery() {
        return new ToolSourceDiscovery(new ReportProperties(
                dapmtRoot.toString(), adminRoot.toString(), "tool-report"));
    }

    private Path projectRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        return java.nio.file.Files.isRegularFile(current.resolve("settings.gradle"))
                ? current : current.getParent();
    }
}
