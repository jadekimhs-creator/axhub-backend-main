package io.shinhanlife.dat.report.excel;

import io.shinhanlife.dat.report.excel.ToolDtoFieldResolver.FieldRow;
import io.shinhanlife.dat.report.model.ToolReportModel;
import io.shinhanlife.dat.report.model.ToolSummary;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * 툴 파일 단위로 3개 시트(툴 상세정보, Request DTO, Response DTO)를 생성하는 Excel 작성기.
 */
@Component
public class ToolReportExcelWriter {

    private static final int DTO_FIRST_FIELD_ROW = 12;
    private static final int DTO_TEMPLATE_LAST_ROW = 35;
    private static final int DTO_COLUMN_COUNT = 20;

    private final ToolDtoFieldResolver fieldResolver = new ToolDtoFieldResolver();

    public byte[] write(List<ToolReportModel> reports) {
        return write(reports, null);
    }

    public byte[] write(List<ToolReportModel> reports, Path sourceRoot) {
        if (reports == null || reports.isEmpty()) {
            throw new IllegalArgumentException("보고서에 포함할 툴이 없습니다.");
        }
        if (reports.size() == 1) {
            return writeSingleTool(reports.get(0), sourceRoot);
        }
        return writeZip(reports, sourceRoot);
    }

    /**
     * 단일 툴 보고서 워크북을 생성하여 byte[]로 반환합니다.
     * 시트 1: 툴 상세정보
     * 시트 2: Request (Input DTO)
     * 시트 3: Response (Output DTO)
     */
    public byte[] writeSingleTool(ToolReportModel report, Path sourceRoot) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            writeToolInfoSheet(workbook, report);
            writeDtoSheet(workbook, "Request (Input DTO)", report, true, sourceRoot);
            writeDtoSheet(workbook, "Response (Output DTO)", report, false, sourceRoot);

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("단일 툴 Excel 보고서 생성에 실패했습니다: " + report.tool().name(), exception);
        }
    }

    /**
     * 복수 툴 보고서를 각 툴별 엑셀 파일로 생성하여 ZIP 압축 파일로 반환합니다.
     */
    public byte[] writeZip(List<ToolReportModel> reports, Path sourceRoot) {
        try (ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(zipBytes)) {

            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            for (ToolReportModel report : reports) {
                String toolName = sanitizeFilename(report.tool().name());
                String title = report.tool().title();
                String titlePart = (title != null && !title.isBlank() && !title.equals(report.tool().name()))
                        ? "(" + sanitizeFilename(title.trim()) + ")"
                        : "(설계서)";
                String entryName = toolName + "_" + titlePart + "_v" + date + ".xlsx";
                byte[] excelContent = writeSingleTool(report, sourceRoot);

                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(excelContent);
                zos.closeEntry();
            }

            zos.finish();
            return zipBytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("툴 보고서 ZIP 생성에 실패했습니다.", exception);
        }
    }

    // =========================================================================
    // 시트 1: 툴 기본정보 및 상세정보
    // =========================================================================

    private void writeToolInfoSheet(XSSFWorkbook workbook, ToolReportModel report) {
        ToolSummary tool = report.tool();
        Sheet sheet = workbook.createSheet("툴 상세정보");
        sheet.setDisplayGridlines(true);

        double[] colWidths = {5, 20, 32, 18, 30, 16, 20, 25};
        for (int i = 0; i < colWidths.length; i++) {
            sheet.setColumnWidth(i, (int) (colWidths[i] * 256));
        }

        CellStyle titleStyle = style(workbook, "000000", "FFFFFF", true, (short) 13, HorizontalAlignment.CENTER, false);
        CellStyle secStyle = style(workbook, "27272A", "FFFFFF", true, (short) 10, HorizontalAlignment.LEFT, true);
        CellStyle labelStyle = borderedStyle(workbook, "F4F4F5", true, HorizontalAlignment.CENTER);
        CellStyle valStyle = borderedStyle(workbook, "FFFFFF", false, HorizontalAlignment.LEFT);
        CellStyle centerValStyle = borderedStyle(workbook, "FFFFFF", false, HorizontalAlignment.CENTER);

        int rowIdx = 0;

        // 1. Title
        Row titleRow = sheet.createRow(rowIdx++);
        titleRow.setHeightInPoints(28);
        for (int c = 0; c < 8; c++) titleRow.createCell(c).setCellStyle(titleStyle);
        String toolTitle = tool.title() != null && !tool.title().isBlank() ? tool.title() : tool.name();
        titleRow.getCell(0).setCellValue("Tool Specification Report · " + toolTitle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        // 2. 기본 식별 정보
        rowIdx = addSectionHeader(sheet, rowIdx, "1. 툴 기본 식별 정보", secStyle);
        rowIdx = add2x2Row(sheet, rowIdx, "툴명", tool.name(), "한글명/제목", tool.title(), labelStyle, valStyle);
        rowIdx = add2x2Row(sheet, rowIdx, "카테고리", tool.categoryKey(), "버전", tool.version() != null && !tool.version().isBlank() ? tool.version() : "1.0.0", labelStyle, valStyle);
        rowIdx = addMergedValueRow(sheet, rowIdx, "목적 및 설명", tool.description(), labelStyle, valStyle);

        // 3. 비즈니스 구현 및 거래 특성 (보완 ⭐)
        rowIdx = addSectionHeader(sheet, rowIdx, "2. 비즈니스 구현 및 거래 특성", secStyle);
        rowIdx = add2x2Row(sheet, rowIdx, "연계 ID (MCI)", tool.mappingId(), "UseCase 클래스", tool.useCaseClass(), labelStyle, valStyle);
        rowIdx = add2x2Row(sheet, rowIdx, "Request DTO", tool.requestType(), "Response DTO", tool.responseType(), labelStyle, valStyle);
        rowIdx = add2x2Row(sheet, rowIdx, "거래 구분", resolveTransactionType(tool), "페이징 방식", resolvePagingType(tool), labelStyle, valStyle);
        rowIdx = add2x2Row(sheet, rowIdx, "승인 필요 여부", tool.requiresApproval() ? "필요 (Y)" : "불필요 (N)",
                "멱등성 (Idempotent)", tool.idempotentHint() || tool.readOnlyHint() ? "보장 (Y)" : "미보장 (N)", labelStyle, valStyle);
        rowIdx = addMergedValueRow(sheet, rowIdx, "원천 파일", tool.sourceFile(), labelStyle, valStyle);
        rowIdx = addMergedValueRow(sheet, rowIdx, "정의 파일", tool.definitionFile(), labelStyle, valStyle);

        // 4. AI Agent 실행 가이드 (LLM Trigger Context) (보완 ⭐)
        rowIdx = addSectionHeader(sheet, rowIdx, "3. AI Agent 실행 가이드 (LLM Trigger Context)", secStyle);
        rowIdx = addMergedValueRow(sheet, rowIdx, "권장 호출 시점", resolveWhenToUse(tool), labelStyle, valStyle);
        rowIdx = addMergedValueRow(sheet, rowIdx, "호출 제외/주의", resolveWhenNotToUse(tool), labelStyle, valStyle);
        rowIdx = addMergedValueRow(sheet, rowIdx, "대표 질의 예시", resolveExampleQueries(tool), labelStyle, valStyle);
        rowIdx = addMergedValueRow(sheet, rowIdx, "입출력 제약", resolveIoLimits(tool), labelStyle, valStyle);
        rowIdx = add2x2Row(sheet, rowIdx, "태그", tool.tags(), "소유 조직", tool.ownerOrg(), labelStyle, valStyle);

        // 5. 오류 및 예외 처리 규격 (Error Handling Spec) (보완 ⭐)
        rowIdx = addSectionHeader(sheet, rowIdx, "4. 오류 및 예외 처리 규격 (Error Handling Spec)", secStyle);
        CellStyle tableHeaderStyle = borderedStyle(workbook, "E4E4E7", true, HorizontalAlignment.CENTER);
        rowIdx = addErrorTableHeader(sheet, rowIdx, tableHeaderStyle);
        rowIdx = addErrorTableRow(sheet, rowIdx, "정상 처리", "SUCCESS", "200 OK", "비즈니스 로직 정상 실행 및 데이터 반환", centerValStyle, valStyle);
        rowIdx = addErrorTableRow(sheet, rowIdx, "조회 결과 없음", "SUCCESS", "200 OK", "검색 조건에 부합하는 데이터 없음 (빈 목록 또는 null 반환)", centerValStyle, valStyle);
        rowIdx = addErrorTableRow(sheet, rowIdx, "파라미터 오류", "FAIL", "400 Bad Request", "필수 입력값 누락 또는 유효성 검증 실패 (단말 메시지 확인)", centerValStyle, valStyle);
        rowIdx = addErrorTableRow(sheet, rowIdx, "MCI 연계 장애", "ERROR", "500 Internal Error", "대내 코어시스템(MCI) 통신 타임아웃 또는 호스트 장애", centerValStyle, valStyle);
        rowIdx = addErrorTableRow(sheet, rowIdx, "권한 없음", "FAIL", "403 Forbidden", "해당 업무/전문 호출 권한 미보유 (권한 부여 필요)", centerValStyle, valStyle);

        // 6. 정책 및 런타임 힌트 (어노테이션 설정)
        rowIdx = addSectionHeader(sheet, rowIdx, "5. 정책 및 런타임 힌트 (어노테이션 설정)", secStyle);
        Row hintHead = sheet.createRow(rowIdx++);
        hintHead.setHeightInPoints(20);
        String[] hintCols = {"등록 여부", "승인 필요", "Read Only", "Destructive", "Idempotent", "Open World"};
        for (int c = 0; c < 6; c++) {
            Cell cl = hintHead.createCell(c + 1);
            cl.setCellStyle(labelStyle);
            cl.setCellValue(hintCols[c]);
        }
        Row hintVal = sheet.createRow(rowIdx++);
        hintVal.setHeightInPoints(20);
        boolean[] hintBools = {tool.register(), tool.requiresApproval(), tool.readOnlyHint(), tool.destructiveHint(), tool.idempotentHint(), tool.openWorldHint()};
        for (int c = 0; c < 6; c++) {
            Cell cv = hintVal.createCell(c + 1);
            cv.setCellStyle(centerValStyle);
            cv.setCellValue(hintBools[c] ? "Y" : "N");
        }

        // 7. 소스 분석 진단
        if (!report.diagnostics().isEmpty()) {
            rowIdx = addSectionHeader(sheet, rowIdx, "6. 분석 진단 및 경고", secStyle);
            for (String diag : report.diagnostics()) {
                rowIdx = addMergedValueRow(sheet, rowIdx, "경고", diag, labelStyle, valStyle);
            }
        }
    }

    private String resolveTransactionType(ToolSummary tool) {
        if (tool.readOnlyHint()) {
            return "조회 (Read-Only)";
        }
        String name = tool.name().toLowerCase();
        if (name.contains("search") || name.contains("inquiry") || name.contains("inquir")
                || name.contains("detail") || name.contains("get") || name.contains("list")
                || name.contains("select") || name.contains("find")) {
            return "조회 (Read-Only)";
        }
        return "등록/변경 (State-Changing)";
    }

    private String resolvePagingType(ToolSummary tool) {
        String combined = (tool.description() + " " + tool.title() + " " + tool.name()).toLowerCase();
        if (combined.contains("스크롤") || combined.contains("scroll") || combined.contains("onbsa1030")) {
            return "스크롤 페이징 (Scroll Paging / 기본 20건)";
        }
        if (combined.contains("페이징") || combined.contains("page")) {
            return "일반 페이징 (Page Paging)";
        }
        return "단건 또는 전체 목록 조회";
    }

    private String resolveWhenToUse(ToolSummary tool) {
        if (tool.whenToUse() != null && !tool.whenToUse().isBlank() && !tool.whenToUse().equals("-")) {
            return tool.whenToUse();
        }
        String title = tool.title() != null && !tool.title().isBlank() ? tool.title() : tool.name();
        return String.format("사용자가 '%s' 관련 정보나 목록 조회를 요청할 때 호출합니다.", title);
    }

    private String resolveWhenNotToUse(ToolSummary tool) {
        if (tool.whenNotToUse() != null && !tool.whenNotToUse().isBlank() && !tool.whenNotToUse().equals("-")) {
            return tool.whenNotToUse();
        }
        return String.format("해당 업무('%s') 목적과 무관한 타 업무 거래나 일반 대화 질의에는 호출하지 않습니다.", tool.categoryKey());
    }

    private String resolveExampleQueries(ToolSummary tool) {
        if (tool.exampleQueries() != null && !tool.exampleQueries().isBlank() && !tool.exampleQueries().equals("-")) {
            return tool.exampleQueries();
        }
        String name = tool.name().toLowerCase();
        if (name.contains("employee") || name.contains("emp")) {
            return "\"홍길동 직원 연락처와 부서 알려줘\", \"디지털기획팀 직원 목록 보여줘\"";
        }
        if (name.contains("customer") || name.contains("cst") || name.contains("individual")) {
            return "\"홍길동 고객 기본정보 조회해줘\", \"고객번호 123456 고객 상세내역 확인해줘\"";
        }
        String title = tool.title() != null && !tool.title().isBlank() ? tool.title() : tool.name();
        return String.format("\"%s 조회해줘\", \"%s 내역 확인해줘\"", title, title);
    }

    private String resolveIoLimits(ToolSummary tool) {
        if (tool.ioLimits() != null && !tool.ioLimits().isBlank() && !tool.ioLimits().equals("-")) {
            return tool.ioLimits();
        }
        if (resolvePagingType(tool).contains("스크롤")) {
            return "1회 호출당 기본 20건 반환 (다음 페이지 존재 시 nextDataExtYn='Y')";
        }
        return "단건 또는 전체 결과 목록 반환";
    }

    private int addErrorTableHeader(Sheet sheet, int rowIndex, CellStyle headerStyle) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22);
        Cell c1 = row.createCell(1); c1.setCellStyle(headerStyle); c1.setCellValue("상태 구분");
        Cell c2 = row.createCell(2); c2.setCellStyle(headerStyle); c2.setCellValue("결과 코드 (resultCode)");
        Cell c3 = row.createCell(3); c3.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 2, 3));
        Cell c4 = row.createCell(4); c4.setCellStyle(headerStyle); c4.setCellValue("HTTP 상태");
        Cell c5 = row.createCell(5); c5.setCellStyle(headerStyle); c5.setCellValue("오류 내용 및 에이전트 대응 가이드");
        Cell c6 = row.createCell(6); c6.setCellStyle(headerStyle);
        Cell c7 = row.createCell(7); c7.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 5, 7));
        return rowIndex + 1;
    }

    private int addErrorTableRow(Sheet sheet, int rowIndex, String type, String resultCode,
                                 String httpStatus, String guide, CellStyle centerStyle, CellStyle valStyle) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(20);
        Cell c1 = row.createCell(1); c1.setCellStyle(centerStyle); c1.setCellValue(type);
        Cell c2 = row.createCell(2); c2.setCellStyle(centerStyle); c2.setCellValue(resultCode);
        Cell c3 = row.createCell(3); c3.setCellStyle(centerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 2, 3));
        Cell c4 = row.createCell(4); c4.setCellStyle(centerStyle); c4.setCellValue(httpStatus);
        Cell c5 = row.createCell(5); c5.setCellStyle(valStyle); c5.setCellValue(guide);
        Cell c6 = row.createCell(6); c6.setCellStyle(valStyle);
        Cell c7 = row.createCell(7); c7.setCellStyle(valStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 5, 7));
        return rowIndex + 1;
    }

    private int addSectionHeader(Sheet sheet, int rowIndex, String title, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(20);
        for (int c = 0; c < 8; c++) row.createCell(c).setCellStyle(style);
        row.getCell(0).setCellValue(title);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 7));
        return rowIndex + 1;
    }

    private int add2x2Row(Sheet sheet, int rowIndex, String label1, String val1,
                          String label2, String val2, CellStyle labelStyle, CellStyle valStyle) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(21);
        Cell l1 = row.createCell(1); l1.setCellStyle(labelStyle); l1.setCellValue(label1);
        Cell v1 = row.createCell(2); v1.setCellStyle(valStyle); v1.setCellValue(val1 == null ? "-" : val1);
        Cell v1b = row.createCell(3); v1b.setCellStyle(valStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 2, 3));

        Cell l2 = row.createCell(4); l2.setCellStyle(labelStyle); l2.setCellValue(label2);
        Cell v2 = row.createCell(5); v2.setCellStyle(valStyle); v2.setCellValue(val2 == null ? "-" : val2);
        Cell v2b = row.createCell(6); v2b.setCellStyle(valStyle);
        Cell v2c = row.createCell(7); v2c.setCellStyle(valStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 5, 7));
        return rowIndex + 1;
    }

    private int addMergedValueRow(Sheet sheet, int rowIndex, String label, String value,
                                  CellStyle labelStyle, CellStyle valStyle) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(21);
        Cell l = row.createCell(1); l.setCellStyle(labelStyle); l.setCellValue(label);
        for (int c = 2; c <= 7; c++) {
            Cell v = row.createCell(c);
            v.setCellStyle(valStyle);
            if (c == 2) v.setCellValue(value == null || value.isBlank() ? "-" : value);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 2, 7));
        return rowIndex + 1;
    }

    // =========================================================================
    // 시트 2, 3: 인터페이스 설계서(대내) 형식 (Request / Response DTO)
    // =========================================================================

    private ToolDtoFieldResolver.ResolvedDtoResult writeDtoSheet(XSSFWorkbook workbook, String sheetName, ToolReportModel report,
                                                                 boolean isInput, Path sourceRoot) {
        ToolSummary tool = report.tool();
        Sheet sheet = workbook.createSheet(sheetName);
        sheet.setDisplayGridlines(false);
        sheet.createFreezePane(0, 12);
        sheet.getPrintSetup().setLandscape(true);
        sheet.setRepeatingRows(new CellRangeAddress(11, 11, -1, -1));

        double[] widths = {4.44, 8, 23.22, 23.22, 10, 14, 18, 11, 9, 7.44,
                8, 9.55, 9, 10.55, 11.44, 9, 13, 10.55, 14, 30};
        for (int column = 0; column < widths.length; column++) {
            sheet.setColumnWidth(column, (int) (widths[column] * 256));
        }

        CellStyle titleStyle = style(workbook, "000000", "FFFFFF", true, (short) 14, HorizontalAlignment.CENTER, false);
        CellStyle sectionStyle = style(workbook, "F2F2F2", "000000", true, (short) 10, HorizontalAlignment.CENTER, false);
        CellStyle labelStyle = borderedStyle(workbook, "F2F2F2", true, HorizontalAlignment.CENTER);
        CellStyle inputStyle = borderedStyle(workbook, "C6D9F1", false, HorizontalAlignment.LEFT);
        CellStyle requiredStyle = borderedStyle(workbook, "C6D9F1", false, HorizontalAlignment.LEFT);
        CellStyle autoStyle = borderedStyle(workbook, "F2DCDB", false, HorizontalAlignment.LEFT);
        CellStyle userStyle = borderedStyle(workbook, "FFFFFF", false, HorizontalAlignment.LEFT);
        CellStyle headerStyle = borderedStyle(workbook, "D9D9D9", true, HorizontalAlignment.CENTER);
        headerStyle.setWrapText(true);
        CellStyle whiteDataStyle = borderedStyle(workbook, "FFFFFF", false, HorizontalAlignment.CENTER);
        CellStyle blueDataStyle = borderedStyle(workbook, "C6D9F1", false, HorizontalAlignment.CENTER);
        CellStyle pinkDataStyle = borderedStyle(workbook, "F2DCDB", false, HorizontalAlignment.CENTER);

        // 1. 헤더 영역 (Title)
        createStyledRow(sheet, 0, 25.5f, titleStyle);
        sheet.addMergedRegion(CellRangeAddress.valueOf("A1:T1"));
        setText(sheet, 0, 0, "인터페이스 설계서(대내) · " + (isInput ? "요청 (Request In)" : "응답 (Response Out)"));

        // 2. 기본정보 헤더
        createStyledRow(sheet, 1, 18f, sectionStyle);
        sheet.addMergedRegion(CellRangeAddress.valueOf("A2:T2"));
        setText(sheet, 1, 0, "기본정보");

        // 3. DTO 해석 (MCI *_I / *_O 우선)
        ToolDtoFieldResolver.ResolvedDtoResult resolved = fieldResolver.resolveDto(tool, isInput, sourceRoot, report.fields());
        String dtoCode = resolved.dtoCode();
        String dtoKoreanName = resolved.dtoKoreanName();
        String dtoEnglishName = resolved.dtoEnglishName();

        String[] labels = {"코드", "한글명", "영문명", "암호화", "유형", "레코드구분자", "필드구분자"};
        for (int index = 0; index < labels.length; index++) {
            int rowIndex = index + 2;
            Row row = sheet.createRow(rowIndex);
            cell(row, 1, labelStyle).setCellValue(labels[index]);
            cell(row, 2, inputStyle);
            cell(row, 3, inputStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 2, 3));
        }
        setText(sheet, 2, 2, dtoCode != null ? dtoCode : "-");
        setText(sheet, 3, 2, dtoKoreanName != null ? dtoKoreanName : "-");
        // 영문명(행 4)은 MciDtoExcelService와 동일하게 비워둠
        setText(sheet, 6, 2, "json");
        sheet.getRow(7).setHeightInPoints(24);
        sheet.getRow(8).setHeightInPoints(24);

        // 범례
        for (int rowIndex = 3; rowIndex <= 5; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            CellStyle legendStyle = rowIndex == 3 ? requiredStyle : rowIndex == 4 ? autoStyle : userStyle;
            cell(row, 5, legendStyle);
            cell(row, 6, legendStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 5, 6));
        }
        setText(sheet, 3, 7, "필수입력");
        setText(sheet, 4, 7, "필드자동채우기(메타시스템 연동시)");
        setText(sheet, 5, 7, "사용자입력(필요시)");

        // 4. 필드정보 헤더
        createStyledRow(sheet, 10, 18f, sectionStyle);
        sheet.addMergedRegion(CellRangeAddress.valueOf("A11:T11"));
        setText(sheet, 10, 0, "필드정보");

        String[] headers = {"NO", "Level", "한글명", "부모식별자(한글명)", "끝수여부", "영문명",
                "부모식별자(영문명)", "데이터유형", "필드길이", "SCALE", "기본값", "정렬기준",
                "채움문자", "암호화방식", "메타체크여부", "한글여부", "소수점포함여부",
                "마스킹여부", "마스킹패턴코드", "비고"};
        Row header = sheet.createRow(11);
        header.setHeightInPoints(30);
        for (int column = 0; column < headers.length; column++) {
            cell(header, column, headerStyle).setCellValue(headers[column]);
        }

        // 5. 기본 템플릿 행 생성
        for (int rowIndex = DTO_FIRST_FIELD_ROW; rowIndex <= DTO_TEMPLATE_LAST_ROW; rowIndex++) {
            Row row = sheet.createRow(rowIndex);
            row.setHeightInPoints(15.75f);
            for (int column = 0; column < DTO_COLUMN_COUNT; column++) {
                CellStyle dataStyle = (column == 0 || column == 4 || (column >= 14 && column <= 16) || column == 19)
                        ? whiteDataStyle
                        : (column >= 1 && column <= 3) ? blueDataStyle : pinkDataStyle;
                cell(row, column, dataStyle);
            }
        }

        // 6. 필드 데이터 채우기
        List<ToolDtoFieldResolver.FieldRow> fields = resolved.fields();
        int requiredRows = Math.max(fields.size(), DTO_TEMPLATE_LAST_ROW - DTO_FIRST_FIELD_ROW + 1);

        for (int offset = 0; offset < requiredRows; offset++) {
            int rowIndex = DTO_FIRST_FIELD_ROW + offset;
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = cloneTemplateRow(sheet, rowIndex);
            }
            clearRowValues(row);
            setNumber(row, 0, offset + 1);
            if (offset < fields.size()) {
                ToolDtoFieldResolver.FieldRow field = fields.get(offset);
                setNumber(row, 1, field.level());
                setText(row, 2, field.description());
                setText(row, 3, field.parentDescription());
                setText(row, 5, field.name());
                setText(row, 6, field.parentName());
                setText(row, 7, field.dataType());
                if (field.length() > 0) {
                    setNumber(row, 8, field.length());
                }
            }
        }
        return resolved;
    }

    private Row cloneTemplateRow(Sheet sheet, int rowIndex) {
        Row source = sheet.getRow(DTO_TEMPLATE_LAST_ROW);
        Row target = sheet.createRow(rowIndex);
        target.setHeight(source.getHeight());
        for (int column = 0; column < DTO_COLUMN_COUNT; column++) {
            Cell sourceCell = source.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            Cell targetCell = target.createCell(column);
            CellStyle style = sourceCell.getCellStyle();
            targetCell.setCellStyle(style);
        }
        return target;
    }

    private void clearRowValues(Row row) {
        for (int column = 0; column < DTO_COLUMN_COUNT; column++) {
            Cell cell = row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setBlank();
        }
    }

    private void createStyledRow(Sheet sheet, int rowIndex, float height, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(height);
        for (int column = 0; column < DTO_COLUMN_COUNT; column++) cell(row, column, style);
    }

    private Cell cell(Row row, int column, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(style);
        return cell;
    }

    private void setText(Sheet sheet, int row, int column, String value) {
        setText(sheet.getRow(row), column, value);
    }

    private void setText(Row row, int column, String value) {
        row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(value == null ? "" : value);
    }

    private void setNumber(Row row, int column, int value) {
        row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(value);
    }

    private CellStyle style(XSSFWorkbook workbook, String fillColor, String fontColor,
                            boolean bold, short fontSize, HorizontalAlignment alignment,
                            boolean bordered) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(java.awt.Color.decode("#" + fillColor), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setFontName("원신한 Light");
        font.setFontHeightInPoints(fontSize);
        font.setBold(bold);
        font.setColor("FFFFFF".equals(fontColor) ? IndexedColors.WHITE.getIndex() : IndexedColors.BLACK.getIndex());
        style.setFont(font);
        if (bordered) setBorders(style);
        return style;
    }

    private CellStyle borderedStyle(XSSFWorkbook workbook, String fillColor,
                                    boolean bold, HorizontalAlignment alignment) {
        return style(workbook, fillColor, "000000", bold, (short) 10, alignment, true);
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "tool";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
