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
    void writesFixedWorkbookStructure() throws Exception {
        ToolSummary tool = new ToolSummary("oth.cst.customer.detail", "고객 상세", "설명", "cst", "ONCSC1340",
                false, false, true, false, false, true, "Request", "Response", "UseCase", "UseCase.java", "", "");
        FieldDefinition field = new FieldDefinition(tool.name(), "TELEGRAM", "INPUT", "ONCSC1340_I",
                "customerNo", "String", true, "고객번호", "length=12", "ONCSC1340_I.java");
        byte[] content = new ToolReportExcelWriter().write(List.of(new ToolReportModel(tool, List.of(field), List.of())));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(4);
            assertThat(workbook.getSheet("툴 기본정보").getRow(8).getCell(1).getStringCellValue()).isEqualTo(tool.name());
            assertThat(workbook.getSheet("툴 정의").getRow(8).getCell(1).getStringCellValue()).isEqualTo(tool.name());
            assertThat(workbook.getSheet("수집 필드").getRow(8).getCell(5).getStringCellValue()).isEqualTo("customerNo");
            assertThat(workbook.getSheet("툴 기본정보").getRow(0).getCell(0).getCellStyle().getFillForegroundColorColor().getARGBHex())
                    .endsWith("262626");
        }
    }
}
