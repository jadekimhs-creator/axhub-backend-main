package io.shinhanlife.dat.mcg.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MciDtoExcelServiceTest {

    private final MciDtoExcelService service = new MciDtoExcelService();

    @Test
    @DisplayName("Workspace Path 및 모듈 기준으로 DTO 목록 스캔 테스트")
    void scanDtoNamesFromWorkspace() {
        String workspacePath = "c:\\eGovFrameDev-4.3.1-64bit\\workspace";
        String moduleName = "dat-was-datps";

        List<String> dtos = service.scanDtoNames(workspacePath, moduleName);

        assertThat(dtos).isNotEmpty();
        assertThat(dtos).contains("ONBSA1030");
    }

    @Test
    @DisplayName("ONBSA1030_I 엑셀 워크북 생성 테스트")
    void generateExcelWorkbookInput() throws Exception {
        String workspacePath = "c:\\eGovFrameDev-4.3.1-64bit\\workspace";
        String moduleName = "dat-was-datps";

        byte[] bytes = service.generateExcelWorkbook(workspacePath, moduleName, "ONBSA1030_I");

        assertThat(bytes).isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetAt(0)).isNotNull();
            assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("대내");
            assertThat(workbook.getSheetAt(0))
                    .anySatisfy(row -> assertThat(row)
                            .anySatisfy(cell -> assertThat(cell.toString()).isEqualTo("inqrCndtValu1")));
        }
    }

    @Test
    @DisplayName("ONBSA1030_O 엑셀 워크북 생성 테스트")
    void generateExcelWorkbookOutput() throws Exception {
        String workspacePath = "c:\\eGovFrameDev-4.3.1-64bit\\workspace";
        String moduleName = "dat-was-datps";

        byte[] bytes = service.generateExcelWorkbook(workspacePath, moduleName, "ONBSA1030_O");

        assertThat(bytes).isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetAt(0)).isNotNull();
            assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("대내");
            assertThat(workbook.getSheetAt(0))
                    .anySatisfy(row -> assertThat(row)
                            .anySatisfy(cell -> assertThat(cell.toString()).isEqualTo("prafNo")));
        }
    }
}
