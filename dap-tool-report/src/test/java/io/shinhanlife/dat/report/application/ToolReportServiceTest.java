package io.shinhanlife.dat.report.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.shinhanlife.dat.report.model.ToolSummary;
import org.junit.jupiter.api.Test;

class ToolReportServiceTest {

    @Test
    void formatsFilenameWithToolNameTitleAndDate() {
        ToolSummary tool = new ToolSummary(
                "pro_pro_contract",
                "계약상태조회",
                "계약 상태를 조회합니다.",
                "pro",
                "CLYMCI00001",
                false, false, true, false, true, false,
                "Request", "Response", "UseCase", "UseCase.java", "", ""
        );

        String filename = ToolReportService.buildReportFilename(tool, "20261021");
        assertThat(filename).isEqualTo("pro_pro_contract_(계약상태조회)_v20261021.xlsx");
    }

    @Test
    void fallbacksToSchemaWhenTitleIsSameAsName() {
        ToolSummary tool = new ToolSummary(
                "pro_pro_contract",
                "pro_pro_contract",
                "설명",
                "pro",
                "",
                false, false, true, false, true, false,
                "Request", "Response", "UseCase", "UseCase.java", "", ""
        );

        String filename = ToolReportService.buildReportFilename(tool, "20261021");
        assertThat(filename).isEqualTo("pro_pro_contract_(설계서)_v20261021.xlsx");
    }

    @Test
    void sanitizesSpecialCharactersInTitleAndName() {
        ToolSummary tool = new ToolSummary(
                "dat.pro/employee:search",
                "인사검색/조회?",
                "설명",
                "pro",
                "",
                false, false, true, false, true, false,
                "Request", "Response", "UseCase", "UseCase.java", "", ""
        );

        String filename = ToolReportService.buildReportFilename(tool, "20261021");
        assertThat(filename).isEqualTo("dat.pro_employee_search_(인사검색_조회_)_v20261021.xlsx");
    }
}
