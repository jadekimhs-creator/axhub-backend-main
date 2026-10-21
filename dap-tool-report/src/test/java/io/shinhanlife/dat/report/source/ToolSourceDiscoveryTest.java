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

    @Test
    void parsesTextBlockAndConcatenation() {
        String code = """
            package io.shinhanlife.dat.mcc.biz.pro.usecase;

            public interface ManuaHrClosiListUseCase {

                @McpTool(
                    name = "pro_search_manual_hr_closing",
                    title = "수작업인사마감특별처리목록 조회",
                    description = \"\"\"
                        [수작업인사마감특별처리 시스템] 수작업인사마감특별처리 채널을 통해 마감년월을 기준으로 수작업인사마감특별처리 내역을 다건 목록으로 조회하는 읽기 전용 도구입니다.\\s
                        본 틀은 마감월도를 기준으로 인사번호, 인사명, 특별처리코드, 특별처리명, 입력 데이터 정보를 다건 목록으로 조회하는 검색(목록) 들입니다.\\s
                        \"\"\"
                )
                @GrowToolHint(
                    requiresApproval = true,
                    categoryKey = "pro",
                    timeoutMillis = 10000L,
                    retryMaxAttempts = 3,
                    mappingId = "DATNBT00002",
                    functionDescription = "[수작업인사마감특별처리 시스템] 마감년월, 조직분류를 입력받아 수작업인사마감특별처리 내역을 다건 목록으로 조회하는 읽기 전용 도구입니다." +
                        "*마감년월(closingYearMonth)은 필수 선행 입력이며, 사용자가 기간을 지정하지 않으면 시스템이 현재 기준 연월로 세팅합니다."
                )
                void testMethod();
            }
            """;
        com.github.javaparser.ast.CompilationUnit unit = null;
        var parser = new com.github.javaparser.JavaParser(
                new com.github.javaparser.ParserConfiguration()
                        .setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_21));
        var parseResult = parser.parse(code);
        assertThat(parseResult.isSuccessful()).isTrue();
        unit = parseResult.getResult().orElseThrow();
        assertThat(unit).isNotNull();
        var method = unit.findAll(com.github.javaparser.ast.body.MethodDeclaration.class).get(0);
        var mcpTool = JavaAnnotationReader.find(method.getAnnotations(), "McpTool").orElseThrow();
        var name = JavaAnnotationReader.string(mcpTool, "name", "fallback");
        var title = JavaAnnotationReader.string(mcpTool, "title", "fallback");
        var desc = JavaAnnotationReader.string(mcpTool, "description", "fallback");
        assertThat(name).isEqualTo("pro_search_manual_hr_closing");
        assertThat(title).isEqualTo("수작업인사마감특별처리목록 조회");
        assertThat(desc).contains("수작업인사마감특별처리 시스템").contains("마감년월");
        var hint = JavaAnnotationReader.find(method.getAnnotations(), "GrowToolHint").orElseThrow();
        var funcDesc = JavaAnnotationReader.string(hint, "functionDescription", "fallback");
        assertThat(funcDesc).contains("수작업인사마감특별처리 시스템").contains("*마감년월(closingYearMonth)");
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
