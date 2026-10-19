package io.shinhanlife.dat.mcg.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * 지정된 Workspace Path 하위의 MCI DTO 소스 파일(*_I.java, *_O.java)을 스캔하고
 * 인터페이스 설계서(대내) 형식의 엑셀 파일을 생성하는 서비스.
 */
@Slf4j
@Service
public class MciDtoExcelService {

    private static final int FIRST_FIELD_ROW = 12;
    private static final int TEMPLATE_LAST_ROW = 35;
    private static final int COLUMN_COUNT = 20;

    private static final Pattern ORDER_PATTERN = Pattern.compile("order\\s*=\\s*(\\d+)");
    private static final Pattern LENGTH_PATTERN = Pattern.compile("length\\s*=\\s*(\\d+)");
    private static final Pattern DESC_PATTERN = Pattern.compile("description\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern TYPE_PATTERN = Pattern.compile("type\\s*=\\s*\"([^\"]*)\"");

    public record FieldRow(int level, String description, String parentDescription,
                           String name, String parentName, String dataType, int length) {
    }

    /**
     * Workspace 경로 및 모듈명 기준으로 DTO baseName 목록을 스캔합니다.
     */
    public List<String> scanDtoNames(String workspacePathStr, String moduleName) {
        Path targetDir = resolveDirectory(workspacePathStr, moduleName);
        if (targetDir == null || !Files.isDirectory(targetDir)) {
            log.warn("Target directory does not exist for scanning DTOs: {}", targetDir);
            return List.of();
        }

        Set<String> baseNames = new TreeSet<>();
        try (Stream<Path> stream = Files.walk(targetDir)) {
            stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> (name.endsWith("_I.java") || name.endsWith("_O.java")) && !name.contains("$"))
                    .forEach(name -> {
                        String simpleName = name.substring(0, name.lastIndexOf('.'));
                        String baseName = simpleName.replaceAll("_[IO]$", "");
                        baseNames.add(baseName);
                    });
        } catch (IOException e) {
            log.error("Failed to scan DTO files in {}", targetDir, e);
        }
        log.info("Scanned {} DTO base names from {}", baseNames.size(), targetDir);
        return List.copyOf(baseNames);
    }

    /**
     * DTO 소스 파일을 찾아 엑셀 워크북 바이트 배열을 생성합니다.
     */
    public byte[] generateExcelWorkbook(String workspacePathStr, String moduleName, String dtoName) throws IOException {
        Path targetDir = resolveDirectory(workspacePathStr, moduleName);
        if (targetDir == null || !Files.isDirectory(targetDir)) {
            throw new IOException("Workspace directory not found: " + targetDir);
        }

        Path dtoFile = findDtoFile(targetDir, dtoName);
        if (dtoFile == null) {
            throw new IOException("DTO source file not found for: " + dtoName);
        }

        String source = Files.readString(dtoFile, StandardCharsets.UTF_8);
        List<FieldRow> fields = describeFields(source, targetDir);

        return createWorkbook(dtoName, fields);
    }

    private Path resolveDirectory(String workspacePathStr, String moduleName) {
        if (workspacePathStr == null || workspacePathStr.isBlank()) {
            workspacePathStr = System.getProperty("AXHUB_SOURCE_DIR");
            if (workspacePathStr == null || workspacePathStr.isBlank()) {
                workspacePathStr = System.getProperty("user.dir");
            }
        }

        Path path = Path.of(workspacePathStr.trim());
        if (moduleName != null && !moduleName.isBlank()) {
            moduleName = moduleName.trim();
            // path 자체가 이미 해당 모듈인 경우
            if (path.getFileName() != null && path.getFileName().toString().equals(moduleName)) {
                return path;
            }
            Path resolved = path.resolve(moduleName);
            if (Files.isDirectory(resolved)) {
                return resolved;
            }
        }
        return path;
    }

    private Path findDtoFile(Path targetDir, String dtoName) {
        String targetFileName = dtoName + ".java";
        try (Stream<Path> stream = Files.walk(targetDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(targetFileName))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            log.error("Error finding DTO file {} in {}", dtoName, targetDir, e);
            return null;
        }
    }

    private List<FieldRow> describeFields(String source, Path searchRoot) {
        List<FieldRow> output = new ArrayList<>();
        Map<String, String> classBodies = extractClassBodies(source);

        // root class 찾기 (가장 첫 번째 class 또는 파일명 매칭)
        String rootClassName = classBodies.keySet().stream().findFirst().orElse("");
        if (rootClassName.isEmpty()) {
            return output;
        }

        appendFields(rootClassName, classBodies, 1, "", "", output, searchRoot);
        return output;
    }

    private void appendFields(String className, Map<String, String> classBodies, int level,
                              String parentName, String parentDescription,
                              List<FieldRow> output, Path searchRoot) {
        String classBody = classBodies.get(className);
        if (classBody == null) {
            // 소스 내에 클래스가 없으면 PageInfo 등 외부 파일에서 찾거나 기본 정의 사용
            if (className.equals("PageInfo")) {
                appendPageInfoFields(level, parentName, parentDescription, output);
                return;
            }
            // 다른 파일에서 소스 탐색
            Path externalFile = findDtoFile(searchRoot, className);
            if (externalFile != null) {
                try {
                    String extSource = Files.readString(externalFile, StandardCharsets.UTF_8);
                    Map<String, String> extBodies = extractClassBodies(extSource);
                    appendFields(className, extBodies, level, parentName, parentDescription, output, searchRoot);
                } catch (IOException ignored) {
                }
            }
            return;
        }

        List<ParsedFieldDef> fields = parseFieldsFromBody(classBody);
        fields.sort(Comparator.comparingInt(f -> f.order));

        for (ParsedFieldDef field : fields) {
            output.add(new FieldRow(level, field.description, parentDescription, field.name,
                    parentName, field.type, field.length));

            if (field.nestedType != null && !field.nestedType.equals(className)) {
                appendFields(field.nestedType, classBodies, level + 1, field.name, field.description, output, searchRoot);
            }
        }
    }

    private void appendPageInfoFields(int level, String parentName, String parentDescription, List<FieldRow> output) {
        output.add(new FieldRow(level, "페이지번호", parentDescription, "pageNo", parentName, "int", 5));
        output.add(new FieldRow(level, "페이지데이터건수", parentDescription, "pageDataCc", parentName, "int", 5));
        output.add(new FieldRow(level, "총페이지수", parentDescription, "totaPageCn", parentName, "int", 10));
        output.add(new FieldRow(level, "총페이지데이터건수", parentDescription, "totaPageDataCc", parentName, "int", 10));
    }

    private Map<String, String> extractClassBodies(String source) {
        Map<String, String> result = new LinkedHashMap<>();
        Pattern classPattern = Pattern.compile("(?m)(?:public\\s+|private\\s+|protected\\s+|static\\s+)*class\\s+([A-Za-z0-9_]+)");
        Matcher matcher = classPattern.matcher(source);

        while (matcher.find()) {
            String cName = matcher.group(1);
            int startIdx = source.indexOf('{', matcher.end());
            if (startIdx == -1) continue;

            int braceCount = 1;
            int currentIdx = startIdx + 1;
            while (currentIdx < source.length() && braceCount > 0) {
                char c = source.charAt(currentIdx);
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                currentIdx++;
            }

            if (braceCount == 0) {
                String body = source.substring(startIdx + 1, currentIdx - 1);
                result.putIfAbsent(cName, body);
            }
        }
        return result;
    }

    private List<ParsedFieldDef> parseFieldsFromBody(String classBody) {
        List<ParsedFieldDef> list = new ArrayList<>();
        // @GlowTrgmField 또는 @GlowMciFieldInfo 로 시작하여 세미콜론으로 끝나는 필드 검색
        Pattern fieldPattern = Pattern.compile(
                "(?s)@(GlowTrgmField|GlowMciFieldInfo)\\s*\\(([^)]*)\\)\\s*(?:@[A-Za-z0-9_]+(?:\\([^)]*\\))?\\s*)*(?:private|protected|public)\\s+([^;=]+?)\\s+([A-Za-z0-9_]+)\\s*(?:=[^;]*)?;");

        Matcher m = fieldPattern.matcher(classBody);
        while (m.find()) {
            String annoContent = m.group(2);
            String rawType = m.group(3).trim();
            String fieldName = m.group(4).trim();

            if (fieldName.equals("serialVersionUID")) continue;

            int order = extractInt(annoContent, ORDER_PATTERN, Integer.MAX_VALUE);
            int length = extractInt(annoContent, LENGTH_PATTERN, 0);
            String desc = extractString(annoContent, DESC_PATTERN, fieldName);
            String typeAttr = extractString(annoContent, TYPE_PATTERN, null);

            String dataType = typeAttr != null ? typeAttr : simpleType(rawType);
            String nestedType = extractNestedType(rawType);

            list.add(new ParsedFieldDef(order, desc, fieldName, dataType, length, nestedType));
        }
        return list;
    }

    private String simpleType(String rawType) {
        if (rawType.startsWith("List<")) {
            return "List";
        }
        int lastDot = rawType.lastIndexOf('.');
        return lastDot != -1 ? rawType.substring(lastDot + 1) : rawType;
    }

    private String extractNestedType(String rawType) {
        if (rawType.startsWith("List<") && rawType.endsWith(">")) {
            String inner = rawType.substring(5, rawType.length() - 1).trim();
            return cleanType(inner);
        }
        String clean = cleanType(rawType);
        if (!isPrimitiveOrJavaLang(clean)) {
            return clean;
        }
        return null;
    }

    private String cleanType(String typeStr) {
        int dot = typeStr.lastIndexOf('.');
        return dot != -1 ? typeStr.substring(dot + 1) : typeStr;
    }

    private boolean isPrimitiveOrJavaLang(String typeName) {
        return switch (typeName) {
            case "String", "int", "Integer", "long", "Long", "double", "Double",
                 "float", "Float", "boolean", "Boolean", "byte", "Byte", "short", "Short",
                 "BigDecimal", "LocalDate", "LocalDateTime", "Date", "Map", "Object" -> true;
            default -> false;
        };
    }

    private int extractInt(String content, Pattern pattern, int fallback) {
        Matcher m = pattern.matcher(content);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private String extractString(String content, Pattern pattern, String fallback) {
        Matcher m = pattern.matcher(content);
        if (m.find()) {
            return m.group(1);
        }
        return fallback;
    }

    private record ParsedFieldDef(int order, String description, String name,
                                  String type, int length, String nestedType) {
    }

    private byte[] createWorkbook(String dtoName, List<FieldRow> fields) throws IOException {
        try (XSSFWorkbook workbook = createTemplateWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            setText(sheet, 2, 2, dtoName);
            setText(sheet, 3, 2, dtoName);

            writeFields(sheet, fields);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private XSSFWorkbook createTemplateWorkbook() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("대내");
        sheet.setDisplayGridlines(false);
        sheet.createFreezePane(0, 12);
        sheet.getPrintSetup().setLandscape(true);
        sheet.setRepeatingRows(new CellRangeAddress(11, 11, -1, -1));

        double[] widths = {4.44, 8, 23.22, 23.22, 10, 14, 18, 11, 9, 7.44,
                8, 9.55, 9, 10.55, 11.44, 9, 13, 10.55, 14, 30};
        for (int column = 0; column < widths.length; column++) {
            sheet.setColumnWidth(column, (int) (widths[column] * 256));
        }

        CellStyle titleStyle = style(workbook, "000000", "FFFFFF", true, (short) 14,
                HorizontalAlignment.CENTER, false);
        CellStyle sectionStyle = style(workbook, "F2F2F2", "000000", true, (short) 10,
                HorizontalAlignment.CENTER, false);
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

        createStyledRow(sheet, 0, 25.5f, titleStyle);
        merge(sheet, "A1:T1");
        setText(sheet, 0, 0, "인터페이스 설계서(대내)");

        createStyledRow(sheet, 1, 18f, sectionStyle);
        merge(sheet, "A2:T2");
        setText(sheet, 1, 0, "기본정보");

        String[] labels = {"코드", "한글명", "영문명", "암호화", "유형", "레코드구분자", "필드구분자"};
        for (int index = 0; index < labels.length; index++) {
            int rowIndex = index + 2;
            Row row = sheet.createRow(rowIndex);
            cell(row, 1, labelStyle).setCellValue(labels[index]);
            cell(row, 2, inputStyle);
            cell(row, 3, inputStyle);
            merge(sheet, "C" + (rowIndex + 1) + ":D" + (rowIndex + 1));
        }
        setText(sheet, 6, 2, "json");
        sheet.getRow(7).setHeightInPoints(24);
        sheet.getRow(8).setHeightInPoints(24);

        for (int rowIndex = 3; rowIndex <= 5; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            CellStyle legendStyle = rowIndex == 3 ? requiredStyle : rowIndex == 4 ? autoStyle : userStyle;
            cell(row, 5, legendStyle);
            cell(row, 6, legendStyle);
            merge(sheet, "F" + (rowIndex + 1) + ":G" + (rowIndex + 1));
        }
        setText(sheet, 3, 7, "필수입력");
        setText(sheet, 4, 7, "필드자동채우기(메타시스템 연동시)");
        setText(sheet, 5, 7, "사용자입력(필요시)");

        createStyledRow(sheet, 10, 18f, sectionStyle);
        merge(sheet, "A11:T11");
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

        for (int rowIndex = FIRST_FIELD_ROW; rowIndex <= TEMPLATE_LAST_ROW; rowIndex++) {
            Row row = sheet.createRow(rowIndex);
            row.setHeightInPoints(15.75f);
            for (int column = 0; column < COLUMN_COUNT; column++) {
                CellStyle dataStyle;
                if (column == 0 || column == 4 || (column >= 14 && column <= 16) || column == 19) {
                    dataStyle = whiteDataStyle;
                } else if (column >= 1 && column <= 3) {
                    dataStyle = blueDataStyle;
                } else {
                    dataStyle = pinkDataStyle;
                }
                cell(row, column, dataStyle);
            }
        }
        return workbook;
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
        font.setFontName("맑은 고딕");
        font.setFontHeightInPoints(fontSize);
        font.setBold(bold);
        font.setColor("FFFFFF".equals(fontColor) ? IndexedColors.WHITE.getIndex() : IndexedColors.BLACK.getIndex());
        style.setFont(font);
        if (bordered) setBorders(style);
        return style;
    }

    private CellStyle borderedStyle(XSSFWorkbook workbook, String fillColor,
                                    boolean bold, HorizontalAlignment alignment) {
        return style(workbook, fillColor, "000000", bold, (short) 9, alignment, true);
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

    private void createStyledRow(Sheet sheet, int rowIndex, float height, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(height);
        for (int column = 0; column < COLUMN_COUNT; column++) cell(row, column, style);
    }

    private Cell cell(Row row, int column, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(style);
        return cell;
    }

    private void merge(Sheet sheet, String range) {
        sheet.addMergedRegion(CellRangeAddress.valueOf(range));
    }

    private void writeFields(Sheet sheet, List<FieldRow> fields) {
        int requiredRows = Math.max(fields.size(), TEMPLATE_LAST_ROW - FIRST_FIELD_ROW + 1);
        for (int offset = 0; offset < requiredRows; offset++) {
            int rowIndex = FIRST_FIELD_ROW + offset;
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = cloneTemplateRow(sheet, rowIndex);
            }
            clearRowValues(row);
            setNumber(row, 0, offset + 1);
            if (offset < fields.size()) {
                FieldRow field = fields.get(offset);
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
    }

    private Row cloneTemplateRow(Sheet sheet, int rowIndex) {
        Row source = sheet.getRow(TEMPLATE_LAST_ROW);
        Row target = sheet.createRow(rowIndex);
        target.setHeight(source.getHeight());
        for (int column = 0; column < COLUMN_COUNT; column++) {
            Cell sourceCell = source.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            Cell targetCell = target.createCell(column);
            CellStyle style = sourceCell.getCellStyle();
            targetCell.setCellStyle(style);
        }
        return target;
    }

    private void clearRowValues(Row row) {
        for (int column = 0; column < COLUMN_COUNT; column++) {
            Cell cell = row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setBlank();
        }
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
}
