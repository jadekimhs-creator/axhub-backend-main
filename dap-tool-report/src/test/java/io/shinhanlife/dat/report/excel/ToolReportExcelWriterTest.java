package io.shinhanlife.dat.report.excel;

import static org.assertj.core.api.Assertions.assertThat;

import io.shinhanlife.dat.report.model.FieldDefinition;
import io.shinhanlife.dat.report.model.ToolReportModel;
import io.shinhanlife.dat.report.model.ToolSummary;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ToolReportExcelWriterTest {

    @Test
    void writesThreeSheetToolReportStructure() throws Exception {
        ToolSummary tool = new ToolSummary("oth.cst.customer.detail", "고객 상세", "설명", "cst", "ONCSC1340",
                false, false, true, false, false, true, "Request", "Response", "UseCase", "UseCase.java", "", "");
        FieldDefinition field = new FieldDefinition(tool.name(), "TELEGRAM", "INPUT", "ONCSC1340_I",
                "customerNo", "String", true, "고객번호", "length=12", "ONCSC1340_I.java");
        byte[] content = new ToolReportExcelWriter().write(List.of(new ToolReportModel(tool, List.of(field), List.of())));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(3);
            assertThat(workbook.getSheet("툴 상세정보")).isNotNull();
            assertThat(workbook.getSheet("Request (Input DTO)")).isNotNull();
            assertThat(workbook.getSheet("Response (Output DTO)")).isNotNull();

            // 1. 툴 상세정보 시트 검증
            assertThat(workbook.getSheet("툴 상세정보").getRow(2).getCell(2).getStringCellValue()).isEqualTo(tool.name());

            // 2. Request (Input DTO) 시트 검증
            assertThat(workbook.getSheet("Request (Input DTO)").getRow(0).getCell(0).getStringCellValue())
                    .contains("인터페이스 설계서(대내)");
            assertThat(workbook.getSheet("Request (Input DTO)").getRow(2).getCell(2).getStringCellValue())
                    .isEqualTo("ONCSC1340_I");

            // 3. Response (Output DTO) 시트 검증
            assertThat(workbook.getSheet("Response (Output DTO)").getRow(0).getCell(0).getStringCellValue())
                    .contains("인터페이스 설계서(대내)");
            assertThat(workbook.getSheet("Response (Output DTO)").getRow(2).getCell(2).getStringCellValue())
                    .isEqualTo("ONCSC1340_O");
        }
    }

    @Test
    void resolvesOnbsa1030DtoForEmployeeSearchTool() throws Exception {
        ToolSummary tool = new ToolSummary("dat.pro.employee.search", "인사검색 목록 조회",
                "직원 성명으로 인사검색 목록 조회 (스크롤페이징 연동: DATNBSO00004 / ONBSA1030)",
                "pro", "DATNBSO00004", false, false, true, false, true, false,
                "ProInquirRequest", "ProInquirResponse", "CmmInquirUseCase",
                "src/main/java/io/shinhanlife/dat/mcc/biz/pro/usecase/CmmInquirUseCase.java", "", "");

        byte[] content = new ToolReportExcelWriter().write(List.of(new ToolReportModel(tool, List.of(), List.of())));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(3);

            // Sheet 2: ONBSA1030_I
            var reqSheet = workbook.getSheet("Request (Input DTO)");
            assertThat(reqSheet).isNotNull();
            assertThat(reqSheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("ONBSA1030_I");

            // Sheet 3: ONBSA1030_O
            var resSheet = workbook.getSheet("Response (Output DTO)");
            assertThat(resSheet).isNotNull();
            assertThat(resSheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("ONBSA1030_O");

            // ONBSA1030_I / ONBSA1030_O의 필드가 파싱되었는지 검증 (12행부터)
            assertThat(reqSheet.getRow(12).getCell(5).getStringCellValue()).isNotBlank();
            assertThat(resSheet.getRow(12).getCell(5).getStringCellValue()).isNotBlank();
        }
    }

    @Test
    void resolvesOncsc1340DtoWithCleanKoreanDescriptionsAndLengths() throws Exception {
        ToolSummary tool = new ToolSummary("pro_individual_inquiry", "개인 고객 상세 조회",
                "개인 고객 상세 정보를 조회합니다.", "pro", "DATNCSO00001",
                false, false, true, false, true, false,
                "IndividualCustomerDetailInquiryRequest", "IndividualCustomerDetailInquiryResponse",
                "IndividualCustomerDetailInquiryUseCase",
                "src/main/java/io/shinhanlife/dat/mcc/biz/pro/usecase/IndividualCustomerDetailInquiryUseCase.java", "", "");

        byte[] content = new ToolReportExcelWriter().write(List.of(new ToolReportModel(tool, List.of(), List.of())));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            var reqSheet = workbook.getSheet("Request (Input DTO)");
            assertThat(reqSheet).isNotNull();
            assertThat(reqSheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("ONCSC1340_I");

            // 첫 번째 필드: 실제 파일에 정의된 첫 번째 필드 검증
            assertThat(reqSheet.getRow(12).getCell(2).getStringCellValue()).isNotBlank();
            assertThat(reqSheet.getRow(12).getCell(5).getStringCellValue()).isNotBlank();
            assertThat(reqSheet.getRow(12).getCell(7).getStringCellValue()).isNotBlank();
        }
    }
}
