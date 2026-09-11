package io.shinhanlife.dat.report.excel;

import io.shinhanlife.dat.report.model.FieldDefinition;
import io.shinhanlife.dat.report.model.ToolReportModel;
import io.shinhanlife.dat.report.model.ToolSummary;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** 모든 보고서 시트에 동일한 전문 I/O 문서 디자인을 적용한다. */
@Component
public class ToolReportExcelWriter {

    private static final int HEADER_ROW = 7;
    private static final int FIRST_DATA_ROW = 8;

    public byte[] write(List<ToolReportModel> reports) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            writeSummary(workbook, reports, styles);
            writeDefinitions(workbook, reports, styles);
            writeFields(workbook, reports, styles);
            writeDiagnostics(workbook, reports, styles);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Excel 보고서 생성에 실패했습니다.", exception);
        }
    }

    private void writeDefinitions(Workbook workbook, List<ToolReportModel> reports, Styles styles) {
        String[] headers = {"No.", "툴명", "버전", "표시 설명", "기능 설명", "사용 시점", "사용 제외",
                "입출력 제한", "예시 질의", "태그", "필수 환경변수", "소유 조직", "정의 파일"};
        Sheet sheet = workbook.createSheet("툴 정의");
        decorateSheet(sheet, "Tool Definition Report", "tool-definitions YAML 메타데이터", reports,
                headers.length, styles);
        header(sheet, headers, styles);
        int rowIndex = FIRST_DATA_ROW;
        int sequence = 1;
        for (ToolReportModel report : reports) {
            ToolSummary tool = report.tool();
            Row row = sheet.createRow(rowIndex++);
            Object[] values = {sequence++, tool.name(), tool.version(), tool.displayDescription(),
                    tool.functionDescription(), tool.whenToUse(), tool.whenNotToUse(), tool.ioLimits(),
                    tool.exampleQueries(), tool.tags(), tool.requiredEnvKeys(), tool.ownerOrg(), tool.definitionFile()};
            values(row, values, styles.body);
            row.getCell(0).setCellStyle(styles.bodyCenter);
            row.getCell(2).setCellStyle(styles.bodyCenter);
        }
        finishTable(sheet, rowIndex, headers.length,
                new int[]{7, 30, 12, 40, 48, 48, 48, 48, 60, 30, 30, 20, 65});
    }

    private void writeSummary(Workbook workbook, List<ToolReportModel> reports, Styles styles) {
        String[] headers = {"No.", "툴명", "제목", "설명", "카테고리", "연계 ID", "등록", "승인 필요",
                "Read Only", "Destructive", "Idempotent", "Open World", "Request", "Response", "UseCase", "원천 파일"};
        Sheet sheet = workbook.createSheet("툴 기본정보");
        decorateSheet(sheet, "MCP Tool Catalog Report", "선택 툴 기본정보", reports, headers.length, styles);
        header(sheet, headers, styles);
        int rowIndex = FIRST_DATA_ROW;
        int sequence = 1;
        for (ToolReportModel report : reports) {
            ToolSummary tool = report.tool();
            Row row = sheet.createRow(rowIndex++);
            Object[] values = {sequence++, tool.name(), tool.title(), tool.description(), tool.categoryKey(),
                    tool.mappingId(), yn(tool.register()), yn(tool.requiresApproval()), yn(tool.readOnlyHint()),
                    yn(tool.destructiveHint()), yn(tool.idempotentHint()), yn(tool.openWorldHint()),
                    tool.requestType(), tool.responseType(), tool.useCaseClass(), tool.sourceFile()};
            values(row, values, styles.body);
            row.getCell(0).setCellStyle(styles.bodyCenter);
        }
        finishTable(sheet, rowIndex, headers.length,
                new int[]{7, 30, 24, 45, 12, 18, 10, 12, 12, 12, 12, 12, 20, 20, 25, 55});
    }

    private void writeFields(Workbook workbook, List<ToolReportModel> reports, Styles styles) {
        String[] headers = {"No.", "툴명", "원천 종류", "방향", "소유 타입", "필드명", "데이터 타입",
                "필수", "설명", "제약조건 / 어노테이션", "원천 파일"};
        Sheet sheet = workbook.createSheet("수집 필드");
        decorateSheet(sheet, "Tool Source I/O Report", "어노테이션 · DTO · 전문 필드", reports, headers.length, styles);
        header(sheet, headers, styles);
        int rowIndex = FIRST_DATA_ROW;
        int sequence = 1;
        for (ToolReportModel report : reports) {
            for (FieldDefinition field : report.fields()) {
                Row row = sheet.createRow(rowIndex++);
                Object[] values = {sequence++, field.toolName(), field.sourceKind(), field.direction(),
                        field.ownerType(), field.fieldName(), field.dataType(),
                        field.required() == null ? "" : yn(field.required()), field.description(),
                        field.constraints(), field.sourceFile()};
                values(row, values, styles.body);
                row.getCell(0).setCellStyle(styles.bodyCenter);
                row.getCell(2).setCellStyle(styles.bodyCenter);
                row.getCell(3).setCellStyle(styles.bodyCenter);
                row.getCell(7).setCellStyle(styles.bodyCenter);
            }
        }
        finishTable(sheet, rowIndex, headers.length,
                new int[]{7, 30, 16, 11, 24, 24, 18, 9, 45, 55, 60});
    }

    private void writeDiagnostics(Workbook workbook, List<ToolReportModel> reports, Styles styles) {
        String[] headers = {"No.", "툴명", "수준", "진단 내용"};
        Sheet sheet = workbook.createSheet("분석 결과");
        decorateSheet(sheet, "Tool Analysis Report", "소스 분석 및 정합성 진단", reports, headers.length, styles);
        header(sheet, headers, styles);
        int rowIndex = FIRST_DATA_ROW;
        int sequence = 1;
        for (ToolReportModel report : reports) {
            if (report.diagnostics().isEmpty()) {
                Row row = sheet.createRow(rowIndex++);
                values(row, new Object[]{sequence++, report.tool().name(), "정상", "분석 경고 없음"}, styles.body);
                row.getCell(0).setCellStyle(styles.bodyCenter);
                row.getCell(2).setCellStyle(styles.bodyCenter);
            } else {
                for (String diagnostic : report.diagnostics()) {
                    Row row = sheet.createRow(rowIndex++);
                    values(row, new Object[]{sequence++, report.tool().name(), "경고", diagnostic}, styles.warning);
                    row.getCell(0).setCellStyle(styles.warningCenter);
                    row.getCell(2).setCellStyle(styles.warningCenter);
                }
            }
        }
        finishTable(sheet, rowIndex, headers.length, new int[]{7, 30, 12, 80});
    }

    private void decorateSheet(Sheet sheet, String titleText, String reportType,
                               List<ToolReportModel> reports, int columnCount, Styles styles) {
        sheet.setDisplayGridlines(false);
        sheet.setAutobreaks(true);
        sheet.getPrintSetup().setLandscape(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
        sheet.setFitToPage(true);

        Row title = sheet.createRow(0);
        title.setHeightInPoints(24);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnCount - 1));
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue(titleText);
        titleCell.setCellStyle(styles.title);

        metadataRow(sheet, 2, "보고서 구분", reportType, columnCount, styles);
        metadataRow(sheet, 3, "선택 툴 수", reports.size(), columnCount, styles);
        metadataRow(sheet, 4, "생성 일시",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), columnCount, styles);
    }

    private void metadataRow(Sheet sheet, int rowIndex, String label, Object value,
                             int columnCount, Styles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(19);
        int labelEnd = Math.min(1, columnCount - 1);
        if (labelEnd > 0) sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, labelEnd));
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.metaLabel);
        int valueStart = labelEnd + 1;
        if (valueStart < columnCount - 1) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, valueStart, columnCount - 1));
        }
        Cell valueCell = row.createCell(valueStart);
        if (value instanceof Number number) valueCell.setCellValue(number.doubleValue());
        else valueCell.setCellValue(value == null ? "" : String.valueOf(value));
        valueCell.setCellStyle(styles.metaValue);
    }

    private void header(Sheet sheet, String[] headers, Styles styles) {
        Row row = sheet.createRow(HEADER_ROW);
        row.setHeightInPoints(30);
        for (int column = 0; column < headers.length; column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(styles.header);
        }
    }

    private void finishTable(Sheet sheet, int rowIndex, int columnCount, int[] characterWidths) {
        widths(sheet, characterWidths);
        sheet.createFreezePane(0, FIRST_DATA_ROW);
        sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW, Math.max(HEADER_ROW, rowIndex - 1), 0, columnCount - 1));
        sheet.setRepeatingRows(new CellRangeAddress(HEADER_ROW, HEADER_ROW, -1, -1));
    }

    private void values(Row row, Object[] values, CellStyle style) {
        row.setHeightInPoints(19);
        for (int column = 0; column < values.length; column++) {
            Cell cell = row.createCell(column);
            Object value = values[column];
            if (value instanceof Number number) cell.setCellValue(number.doubleValue());
            else cell.setCellValue(value == null ? "" : String.valueOf(value));
            cell.setCellStyle(style);
        }
    }

    private void widths(Sheet sheet, int[] characterWidths) {
        for (int i = 0; i < characterWidths.length; i++) {
            sheet.setColumnWidth(i, Math.min(255, characterWidths[i]) * 256);
        }
    }

    private String yn(boolean value) {
        return value ? "Y" : "N";
    }

    private static final class Styles {
        private final CellStyle title;
        private final CellStyle metaLabel;
        private final CellStyle metaValue;
        private final CellStyle header;
        private final CellStyle body;
        private final CellStyle bodyCenter;
        private final CellStyle warning;
        private final CellStyle warningCenter;

        private Styles(Workbook workbook) {
            title = style(workbook, "#262626", "#FFFFFF", 12, true, HorizontalAlignment.CENTER, false);
            metaLabel = style(workbook, "#333333", "#FFFFFF", 10, true, HorizontalAlignment.CENTER, false);
            metaValue = style(workbook, "#FFFFFF", "#222222", 10, false, HorizontalAlignment.LEFT, false);
            header = style(workbook, "#404040", "#FFFFFF", 9, true, HorizontalAlignment.CENTER, true);
            body = style(workbook, "#FFFFFF", "#222222", 9, false, HorizontalAlignment.LEFT, true);
            bodyCenter = style(workbook, "#FFFFFF", "#222222", 9, false, HorizontalAlignment.CENTER, true);
            warning = style(workbook, "#FFF2CC", "#7F6000", 9, false, HorizontalAlignment.LEFT, true);
            warningCenter = style(workbook, "#FFF2CC", "#7F6000", 9, true, HorizontalAlignment.CENTER, true);
        }

        private CellStyle style(Workbook workbook, String fill, String fontColor, int size, boolean bold,
                                HorizontalAlignment alignment, boolean borders) {
            XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
            style.setFillForegroundColor(color(fill));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(alignment);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setWrapText(true);
            Font font = workbook.createFont();
            font.setFontName("Carlito");
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            ((org.apache.poi.xssf.usermodel.XSSFFont) font).setColor(color(fontColor));
            style.setFont(font);
            if (borders) applyBorders(style);
            return style;
        }

        private void applyBorders(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            short borderColor = org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex();
            style.setTopBorderColor(borderColor);
            style.setBottomBorderColor(borderColor);
            style.setLeftBorderColor(borderColor);
            style.setRightBorderColor(borderColor);
        }

        private XSSFColor color(String hex) {
            return new XSSFColor(Color.decode(hex), new DefaultIndexedColorMap());
        }
    }
}
