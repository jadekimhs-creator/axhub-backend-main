package io.shinhanlife.dat.report.source;

import static org.assertj.core.api.Assertions.assertThat;

import io.shinhanlife.dat.report.config.ReportProperties;
import io.shinhanlife.dat.report.model.ToolSourceType;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * @package io.shinhanlife.dat.report.source
 * @className ToolSourceDiscoveryTest
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 * 
 * </pre>
 */
class ToolSourceDiscoveryTest {

    private final Path adminRoot = projectRoot();
    private final Path dapmtRoot = adminRoot.resolve("../dat-was-datmt").normalize();

    @Test
    void discoversToolsFromBothSelectableProjects() {
        ToolSourceDiscovery discovery = discovery();

        assertThat(discovery.discover(ToolSourceType.DAP_ADMIN))
                .extracting(tool -> tool.name())
                .contains("cmm_claim_search");
        assertThat(discovery.discover(ToolSourceType.DAP_WAS_DAPMT))
                .extracting(tool -> tool.name())
                .contains("pro_recruitment_data");
    }

    @Test
    void collectsParametersFromDtoAnnotationsInsteadOfYamlDefinitions() {
        ToolSourceDiscovery discovery = discovery();
        var tool = discovery.discover(ToolSourceType.DAP_WAS_DAPMT).stream()
                .filter(candidate -> candidate.name().equals("pro_recruitment_data"))
                .findFirst().orElseThrow();

        var report = new ToolDetailAnalyzer(discovery).analyze(tool, ToolSourceType.DAP_WAS_DAPMT);

        assertThat(report.fields()).anySatisfy(field -> {
            assertThat(field.sourceKind()).isEqualTo("DTO");
            assertThat(field.direction()).isEqualTo("INPUT");
            assertThat(field.fieldName()).isEqualTo("companyCode");
            assertThat(field.constraints()).contains("Schema=");
        });
        assertThat(report.fields()).noneMatch(field -> field.sourceKind().equals("TOOL_DEFINITION"));
    }

    @Test
    void discoversToolsFromCustomWorkspaceModule() {
        Path datpsPath = Path.of("C:/eGovFrameDev-4.3.1-64bit/workspace/dat-was-datps");
        if (java.nio.file.Files.isDirectory(datpsPath)) {
            ToolSourceDiscovery discovery = discovery();
            var tools = discovery.discover(datpsPath);
            assertThat(tools)
                    .extracting(tool -> tool.name())
                    .contains("pro_pro_contract", "pro_individual_inquiry", "pro_recruitment_data");
        }
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
