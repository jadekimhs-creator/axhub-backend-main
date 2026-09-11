package io.shinhanlife.dat.mcg.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dat.lib.dto.OperationType;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentGeneratorServiceTest {

    private DocumentGeneratorService service;
    private ToolMetadata tool;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-11T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new DocumentGeneratorService(new ObjectMapper(), clock);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("customerId", Map.of(
                "type", "string",
                "description", "고객 ID",
                "pattern", "\\d{8}",
                "examples", List.of("12345678")));
        properties.put("pageSize", Map.of(
                "type", "integer",
                "description", "페이지 크기",
                "default", 20));

        tool = ToolMetadata.builder()
                .uid("ebf042d9-2203-3992-9237-634a58515223")
                .semver("1.0.0")
                .displayName("테스트 고객조회 Tool")
                .name("oth.cmm.customer.detail")
                .description("테스트 고객의 상세정보를 조회합니다.")
                .categoryKey("cmm")
                .endpoint("http://was-cus:8084/mcp/oth.cmm.customer.detail")
                .podUrl("http://was-cus:8084")
                .integrationType("REST")
                .mciServiceId("CUST_001")
                .operationType(OperationType.READ)
                .timeoutMillis(5000L)
                .visible(true)
                .parametersSchema(Map.of(
                        "type", "object",
                        "properties", properties,
                        "required", List.of("customerId")))
                .build();
    }

    @Test
    void createsSelectedProgramSheetsFromTemplate() throws Exception {
        GeneratedDocument document = service.generateProgram(
                new DocumentGenerationRequest(tool, "1.0", true, true, true));

        assertThat(document.fileName())
                .isEqualTo("테스트 고객조회 Tool_프로그램정의서_v1.0_20260811.xlsx");
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(document.content()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(3);
            assertThat(workbook.getSheetName(0)).isEqualTo("프로그램정의서");
            assertThat(workbook.getSheetName(1)).isEqualTo("처리설계");
            assertThat(workbook.getSheetName(2)).isEqualTo("개정이력");
            assertThat(workbook.getSheet("프로그램정의서").getRow(4).getCell(1).getStringCellValue())
                    .isEqualTo("테스트 고객조회 Tool");
            assertThat(workbook.getSheet("처리설계").getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("처리설계");
            assertThat(workbook.getSheet("개정이력").getRow(3).getCell(0).getStringCellValue())
                    .isEqualTo("1.0");
            assertThat(workbook.getNumCellStyles()).isGreaterThan(8);
        }
    }

    @Test
    void removesUncheckedProgramSheets() throws Exception {
        GeneratedDocument document = service.generateProgram(
                new DocumentGenerationRequest(tool, "v2.0", false, false, true));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(document.content()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            assertThat(workbook.getSheetName(0)).isEqualTo("개정이력");
            assertThat(workbook.getSheetAt(0).getRow(3).getCell(0).getStringCellValue()).isEqualTo("2.0");
        }
    }

    @Test
    void createsRequestAndResponseInterfaceSheetsFromTemplate() throws Exception {
        GeneratedDocument document = service.generateInterface(
                new DocumentGenerationRequest(tool, "1.0", false, false, false));

        assertThat(document.fileName())
                .isEqualTo("테스트 고객조회 Tool_인터페이스정의서_v1.0_20260811.xlsx");
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(document.content()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("Request In");
            assertThat(workbook.getSheetName(1)).isEqualTo("Response Out");
            assertThat(workbook.getSheet("Request In").getRow(1).getCell(2).getStringCellValue())
                    .isEqualTo("테스트 고객조회 Tool");
            assertThat(workbook.getSheet("Request In").getRow(3).getCell(3).getStringCellValue())
                    .isEqualTo("http://was-cus:8084/mcp/oth.cmm.customer.detail");
            assertThat(workbook.getSheet("Request In").getRow(8).getCell(5).getStringCellValue())
                    .isEqualTo("customerId");
            assertThat(workbook.getSheet("Request In").getRow(8).getCell(7).getStringCellValue())
                    .isEqualTo("8");
            assertThat(workbook.getSheet("Request In").getRow(32).getCell(2).getStringCellValue())
                    .contains("customerId", "12345678");
            assertThat(workbook.getSheet("Response Out").getRow(8).getCell(5).getStringCellValue())
                    .isEqualTo("resultData");
        }
    }

    @Test
    void rejectsProgramRequestWithoutSelectedSheet() {
        assertThatThrownBy(() -> service.generateProgram(
                new DocumentGenerationRequest(tool, "1.0", false, false, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("한 개 이상의 시트");
    }
}
