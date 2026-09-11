package io.shinhanlife.dat.mcg.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentGeneratorService {

    static final String PROGRAM_TEMPLATE = "document-templates/excel/program-definition-template.xlsx";
    static final String INTERFACE_TEMPLATE = "document-templates/excel/interface-definition-template.xlsx";

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Pattern PATTERN_LENGTH = Pattern.compile("\\\\d\\{(\\d+)}");
    private static final int INTERFACE_FIRST_ROW = 8;
    private static final int INTERFACE_LAST_ROW = 30;

    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public DocumentGeneratorService(ObjectMapper objectMapper) {
        this(objectMapper, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    DocumentGeneratorService(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public GeneratedDocument generateProgram(DocumentGenerationRequest request) {
        ToolMetadata tool = validate(request);
        if (!request.includeProgram() && !request.includeProcess() && !request.includeRevision()) {
            throw new IllegalArgumentException("프로그램 문서에서 한 개 이상의 시트를 선택하세요.");
        }

        String version = normalizeVersion(request.version());
        LocalDate today = LocalDate.now(clock);
        try (InputStream input = resource(PROGRAM_TEMPLATE);
             Workbook workbook = WorkbookFactory.create(input)) {

            Sheet programSheet = requiredSheet(workbook, "프로그램정의서");
            Sheet designTemplate = requiredSheet(workbook, "입출력정의");
            Sheet revisionSheet = request.includeRevision()
                    ? workbook.cloneSheet(workbook.getSheetIndex(designTemplate))
                    : null;

            if (request.includeProgram()) {
                populateProgramSheet(programSheet, tool, version, today);
            } else {
                workbook.removeSheetAt(workbook.getSheetIndex(programSheet));
            }

            if (request.includeProcess()) {
                workbook.setSheetName(workbook.getSheetIndex(designTemplate), "처리설계");
                populateProcessSheet(designTemplate, tool);
            } else {
                workbook.removeSheetAt(workbook.getSheetIndex(designTemplate));
            }

            if (revisionSheet != null) {
                workbook.setSheetName(workbook.getSheetIndex(revisionSheet), "개정이력");
                populateRevisionSheet(revisionSheet, version, today);
            }

            workbook.setActiveSheet(0);
            return output(workbook, programFileName(tool, version, today));
        } catch (IOException exception) {
            throw new IllegalStateException("프로그램정의서 Excel 생성에 실패했습니다.", exception);
        }
    }

    public GeneratedDocument generateInterface(DocumentGenerationRequest request) {
        ToolMetadata tool = validate(request);
        String version = normalizeVersion(request.version());
        LocalDate today = LocalDate.now(clock);
        try (InputStream input = resource(INTERFACE_TEMPLATE);
             Workbook workbook = WorkbookFactory.create(input)) {

            populateInterfaceSheet(requiredSheet(workbook, "Request In"), tool, false);
            populateInterfaceSheet(requiredSheet(workbook, "Response Out"), tool, true);
            workbook.setActiveSheet(0);
            return output(workbook, interfaceFileName(tool, version, today));
        } catch (IOException exception) {
            throw new IllegalStateException("인터페이스정의서 Excel 생성에 실패했습니다.", exception);
        }
    }

    private void populateProgramSheet(Sheet sheet, ToolMetadata tool, String version, LocalDate today) {
        String label = label(tool);
        set(sheet, "B4", documentId(tool));
        set(sheet, "E4", version);
        set(sheet, "H4", DISPLAY_DATE.format(today));
        set(sheet, "B5", label);
        set(sheet, "E5", value(tool.getName()));
        set(sheet, "H5", "생성 완료");
        set(sheet, "B6", value(tool.getCategoryKey()));
        set(sheet, "E6", "AX HUB MCP Gateway");
        set(sheet, "H6", "자동 생성");
        set(sheet, "B9", defaultValue(tool.getDescription(), "설명이 등록되지 않은 Tool입니다."));
        set(sheet, "B10", label);
        set(sheet, "E10", tool.getOperationType() == null ? "-" : tool.getOperationType().name());
        set(sheet, "H10", Boolean.FALSE.equals(tool.getVisible()) ? "비공개" : "공개");
        set(sheet, "B11", schemaFields(tool.getParametersSchema()).size()
                + "개 파라미터가 Tool JSON 스키마에서 자동 매핑되었습니다.");
        set(sheet, "B14", value(tool.getCategoryKey()));
        set(sheet, "E14", value(tool.getPodUrl()));
        set(sheet, "H14", value(tool.getIntegrationType()));
        set(sheet, "B15", value(tool.getEndpoint()));
        set(sheet, "E15", value(tool.getMciServiceId()));
        set(sheet, "H15", formatTimeout(tool.getTimeoutMillis()));
        set(sheet, "B16", yesNo(tool.getRequiresApproval()));
        set(sheet, "E16", yesNo(tool.getReadOnlyHint()));
        set(sheet, "H16", yesNo(tool.getIdempotentHint()));
        set(sheet, "B21", label);
        set(sheet, "C21", "Tool 입력 스키마에 따라 요청 파라미터를 검증합니다.");
        set(sheet, "B22", defaultValue(tool.getIntegrationType(), "REST"));
        set(sheet, "H22", value(tool.getMciServiceId()));
        set(sheet, "A26", "※ ToolMetadata를 기준으로 자동 생성된 문서입니다. 업무 규칙과 승인 정보는 담당자 검토가 필요합니다.");
    }

    private void populateProcessSheet(Sheet sheet, ToolMetadata tool) {
        CellStyle title = style(sheet, "A1");
        CellStyle section = style(sheet, "A2");
        CellStyle header = style(sheet, "A3");
        CellStyle data = style(sheet, "A4");
        CellStyle note = style(sheet, "A9");
        resetSheet(sheet, 9, 9);

        mergeSet(sheet, "A1:I1", "처리설계", title);
        mergeSet(sheet, "A2:I2", label(tool) + " · 공통 처리 절차", section);
        setStyled(sheet, 2, 0, "단계", header);
        setStyled(sheet, 2, 1, "처리 주체", header);
        mergeSet(sheet, "C3:F3", "처리 내용", header);
        mergeSet(sheet, "G3:H3", "성공 조건", header);
        setStyled(sheet, 2, 8, "비고", header);

        List<List<String>> steps = List.of(
                List.of("1", "Gateway", "호출자 인증과 Tool 실행 권한을 확인합니다.", "권한 검증 성공", "공통 처리"),
                List.of("2", label(tool), "Tool 입력 스키마에 따라 요청 파라미터를 검증합니다.", "스키마 검증 성공", "자동 생성"),
                List.of("3", defaultValue(tool.getIntegrationType(), "REST"), "등록된 엔드포인트 또는 서비스 ID로 대상 시스템을 호출합니다.", "정상 응답 수신", value(tool.getMciServiceId())),
                List.of("4", "Gateway", "응답을 MCP 표준 결과로 변환하고 정책을 검사합니다.", "응답 정책 통과", "응답 정책"),
                List.of("5", "Gateway", "감사 로그를 기록하고 호출자에게 결과를 반환합니다.", "응답 전송 완료", "추적 ID 포함")
        );
        for (int index = 0; index < steps.size(); index++) {
            int row = 3 + index;
            List<String> step = steps.get(index);
            setStyled(sheet, row, 0, step.get(0), data);
            setStyled(sheet, row, 1, step.get(1), data);
            mergeSet(sheet, "C" + (row + 1) + ":F" + (row + 1), step.get(2), data);
            mergeSet(sheet, "G" + (row + 1) + ":H" + (row + 1), step.get(3), data);
            setStyled(sheet, row, 8, step.get(4), data);
            sheet.getRow(row).setHeightInPoints(38);
        }
        mergeSet(sheet, "A9:I9", "※ 공통 처리 흐름은 ToolMetadata와 Gateway 정책을 기준으로 자동 작성되었습니다.", note);
    }

    private void populateRevisionSheet(Sheet sheet, String version, LocalDate today) {
        CellStyle title = style(sheet, "A1");
        CellStyle section = style(sheet, "A2");
        CellStyle header = style(sheet, "A3");
        CellStyle data = style(sheet, "A4");
        CellStyle note = style(sheet, "A9");
        resetSheet(sheet, 9, 9);

        mergeSet(sheet, "A1:I1", "개정이력", title);
        mergeSet(sheet, "A2:I2", "문서 버전 및 변경 내역", section);
        setStyled(sheet, 2, 0, "버전", header);
        setStyled(sheet, 2, 1, "작성일", header);
        mergeSet(sheet, "C3:D3", "작성자", header);
        mergeSet(sheet, "E3:H3", "변경 내용", header);
        setStyled(sheet, 2, 8, "비고", header);

        setStyled(sheet, 3, 0, version, data);
        setStyled(sheet, 3, 1, DISPLAY_DATE.format(today), data);
        mergeSet(sheet, "C4:D4", "Document Generator", data);
        mergeSet(sheet, "E4:H4", "ToolMetadata 기준 최초 생성", data);
        setStyled(sheet, 3, 8, "자동 생성", data);
        mergeSet(sheet, "A6:I6", "※ 배포 전 담당자의 최종 검토가 필요합니다.", note);
    }

    private void populateInterfaceSheet(Sheet sheet, ToolMetadata tool, boolean response) {
        String label = label(tool);
        String interfaceId = interfaceId(tool);
        set(sheet, "A1", label + " 인터페이스 설계서");
        set(sheet, "C2", label);
        set(sheet, "C3", defaultValue(tool.getDescription(), "설명이 등록되지 않은 Tool입니다."));
        set(sheet, "D4", value(tool.getEndpoint()));
        set(sheet, "D5", "운영 URL 확인 필요");
        set(sheet, "A7", interfaceId);
        set(sheet, "B7", response ? "데이터 수신시스템 · 응답 (Response Out)" : "데이터 송신시스템 · 요청 (Request In)");

        clearInterfaceRows(sheet);
        if (response) {
            writeInterfaceField(sheet, INTERFACE_FIRST_ROW, interfaceId, "AX HUB\nMCP Gateway", "Body",
                    interfaceId + "_O", new SchemaField("resultData", "object", "결과 데이터", "-", true, "", false));
            set(sheet, "C33", prettyJson(Map.of("resultData", Map.of("status", "SUCCESS"))));
        } else {
            List<SchemaField> fields = schemaFields(tool.getParametersSchema());
            if (fields.size() > INTERFACE_LAST_ROW - INTERFACE_FIRST_ROW + 1) {
                throw new IllegalArgumentException("인터페이스 양식은 최대 23개 요청 필드를 지원합니다.");
            }
            for (int index = 0; index < fields.size(); index++) {
                writeInterfaceField(sheet, INTERFACE_FIRST_ROW + index, interfaceId,
                        defaultValue(tool.getIntegrationType(), "Tool"), "Body", interfaceId + "_I", fields.get(index));
            }
            set(sheet, "C33", prettyJson(exampleFromSchema(tool.getParametersSchema())));
        }
        set(sheet, "A34", "※ ToolMetadata를 기준으로 자동 생성된 검토용 문서입니다.");
    }

    private void writeInterfaceField(Sheet sheet, int rowIndex, String interfaceId, String system, String level,
                                     String store, SchemaField field) {
        if (rowIndex == INTERFACE_FIRST_ROW) {
            setStyled(sheet, rowIndex, 0, interfaceId, style(sheet, "A9"));
            setStyled(sheet, rowIndex, 1, system, style(sheet, "B9"));
            setStyled(sheet, rowIndex, 2, level, style(sheet, "C9"));
        }
        setStyled(sheet, rowIndex, 3, store, style(sheet, "D9"));
        setStyled(sheet, rowIndex, 4, defaultValue(field.description(), field.path()), style(sheet, "E9"));
        setStyled(sheet, rowIndex, 5, field.path(), style(sheet, "F9"));
        setStyled(sheet, rowIndex, 6, field.type(), style(sheet, "G9"));
        setStyled(sheet, rowIndex, 7, field.length(), style(sheet, "H9"));
        setStyled(sheet, rowIndex, 8, "", style(sheet, "I9"));
        setStyled(sheet, rowIndex, 9, field.coded() ? "Y" : "N", style(sheet, "J9"));
        String note = (field.required() ? "필수 · " : "") + "ToolMetadata";
        if (!field.example().isBlank()) {
            note += " · 예시: " + field.example();
        }
        setStyled(sheet, rowIndex, 10, note, style(sheet, "K9"));
    }

    private void clearInterfaceRows(Sheet sheet) {
        for (int rowIndex = INTERFACE_FIRST_ROW; rowIndex <= INTERFACE_LAST_ROW; rowIndex++) {
            for (int column = 3; column <= 10; column++) {
                setStyled(sheet, rowIndex, column, "", style(sheet, "D9"));
            }
        }
        set(sheet, "A9", "");
        set(sheet, "B9", "");
        set(sheet, "C9", "");
    }

    private List<SchemaField> schemaFields(Map<String, Object> schema) {
        List<SchemaField> fields = new ArrayList<>();
        collectSchemaFields(schema, "", Set.of(), fields);
        return fields;
    }

    @SuppressWarnings("unchecked")
    private void collectSchemaFields(Map<String, Object> schema, String prefix, Set<String> inheritedRequired,
                                     List<SchemaField> fields) {
        if (schema == null) {
            return;
        }
        Object requiredValue = schema.get("required");
        Set<String> required = requiredValue instanceof Collection<?> values
                ? values.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet())
                : inheritedRequired;
        Object propertiesValue = schema.get("properties");
        if (!(propertiesValue instanceof Map<?, ?> properties)) {
            return;
        }
        for (Map.Entry<?, ?> entry : properties.entrySet()) {
            String name = String.valueOf(entry.getKey());
            if (!(entry.getValue() instanceof Map<?, ?> rawNode)) {
                continue;
            }
            Map<String, Object> node = (Map<String, Object>) rawNode;
            String path = prefix.isBlank() ? name : prefix + "." + name;
            String type = String.valueOf(node.getOrDefault("type", "string"));
            String description = value(node.get("description"));
            String length = schemaLength(node);
            String example = schemaExample(node);
            boolean coded = node.get("enum") instanceof Collection<?> values && !values.isEmpty();
            fields.add(new SchemaField(path, type, description, length, required.contains(name), example, coded));
            if ("object".equals(type)) {
                collectSchemaFields(node, path, Set.of(), fields);
            } else if ("array".equals(type) && node.get("items") instanceof Map<?, ?> items) {
                collectSchemaFields((Map<String, Object>) items, path + "[]", Set.of(), fields);
            }
        }
    }

    private String schemaLength(Map<String, Object> node) {
        Object length = node.get("maxLength");
        if (length == null) {
            length = node.get("length");
        }
        if (length != null) {
            return String.valueOf(length);
        }
        Object pattern = node.get("pattern");
        if (pattern != null) {
            Matcher matcher = PATTERN_LENGTH.matcher(String.valueOf(pattern));
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "-";
    }

    private String schemaExample(Map<String, Object> node) {
        Object example = node.get("example");
        if (example == null && node.get("examples") instanceof List<?> examples && !examples.isEmpty()) {
            example = examples.get(0);
        }
        if (example == null) {
            example = node.get("default");
        }
        return value(example);
    }

    @SuppressWarnings("unchecked")
    private Object exampleFromSchema(Map<String, Object> schema) {
        if (schema == null) {
            return Map.of();
        }
        Object type = schema.get("type");
        if ("object".equals(type) || schema.get("properties") instanceof Map<?, ?>) {
            Map<String, Object> result = new LinkedHashMap<>();
            Object propertiesValue = schema.get("properties");
            if (propertiesValue instanceof Map<?, ?> properties) {
                for (Map.Entry<?, ?> entry : properties.entrySet()) {
                    if (entry.getValue() instanceof Map<?, ?> node) {
                        result.put(String.valueOf(entry.getKey()), exampleFromSchema((Map<String, Object>) node));
                    }
                }
            }
            return result;
        }
        Object example = schema.get("example");
        if (example == null && schema.get("examples") instanceof List<?> examples && !examples.isEmpty()) {
            example = examples.get(0);
        }
        if (example == null) {
            example = schema.get("default");
        }
        if (example != null) {
            return example;
        }
        return switch (String.valueOf(type)) {
            case "integer", "number" -> 0;
            case "boolean" -> false;
            case "array" -> schema.get("items") instanceof Map<?, ?> items
                    ? List.of(exampleFromSchema((Map<String, Object>) items)) : List.of();
            default -> "<value>";
        };
    }

    private String prettyJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON 예시 생성에 실패했습니다.", exception);
        }
    }

    private GeneratedDocument output(Workbook workbook, String fileName) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.write(output);
            return new GeneratedDocument(fileName, output.toByteArray());
        }
    }

    private InputStream resource(String path) throws IOException {
        return new ClassPathResource(path).getInputStream();
    }

    private ToolMetadata validate(DocumentGenerationRequest request) {
        if (request == null || request.tool() == null) {
            throw new IllegalArgumentException("ToolMetadata가 필요합니다.");
        }
        ToolMetadata tool = request.tool();
        if (isBlank(tool.getUid()) && isBlank(tool.getName())) {
            throw new IllegalArgumentException("Tool UID 또는 Tool 이름이 필요합니다.");
        }
        return tool;
    }

    private String programFileName(ToolMetadata tool, String version, LocalDate today) {
        return safeFilename(label(tool)) + "_프로그램정의서_v" + version + "_" + FILE_DATE.format(today) + ".xlsx";
    }

    private String interfaceFileName(ToolMetadata tool, String version, LocalDate today) {
        return safeFilename(label(tool)) + "_인터페이스정의서_v" + version + "_" + FILE_DATE.format(today) + ".xlsx";
    }

    private String normalizeVersion(String version) {
        String normalized = isBlank(version) ? "1.0" : version.trim().replaceFirst("^[vV]", "");
        if (!normalized.matches("[0-9A-Za-z._-]+")) {
            throw new IllegalArgumentException("버전은 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.");
        }
        return normalized;
    }

    private String safeFilename(String value) {
        String result = defaultValue(value, "tool").replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return result.isEmpty() ? "tool" : result;
    }

    private String label(ToolMetadata tool) {
        return defaultValue(tool.getDisplayName(), defaultValue(tool.getName(), tool.getUid()));
    }

    private String documentId(ToolMetadata tool) {
        String category = defaultValue(tool.getCategoryKey(), "ETC").toUpperCase(Locale.ROOT);
        String uid = defaultValue(tool.getUid(), tool.getName()).replaceAll("[^0-9A-Za-z]", "");
        uid = uid.length() > 7 ? uid.substring(0, 7) : uid;
        return "AXHUB-FS-" + category + "-" + uid.toUpperCase(Locale.ROOT);
    }

    private String interfaceId(ToolMetadata tool) {
        int hash = Objects.hash(tool.getUid(), tool.getName());
        return "AXHUB" + String.format(Locale.ROOT, "%05d", Math.floorMod(hash, 100_000));
    }

    private String formatTimeout(Long timeout) {
        return timeout == null ? "-" : String.format(Locale.ROOT, "%,d ms", timeout);
    }

    private String yesNo(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Y" : "N";
    }

    private Sheet requiredSheet(Workbook workbook, String name) {
        Sheet sheet = workbook.getSheet(name);
        if (sheet == null) {
            throw new IllegalStateException("Excel 템플릿에 '" + name + "' 시트가 없습니다.");
        }
        return sheet;
    }

    private void resetSheet(Sheet sheet, int rows, int columns) {
        for (int index = sheet.getNumMergedRegions() - 1; index >= 0; index--) {
            sheet.removeMergedRegion(index);
        }
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            Row row = row(sheet, rowIndex);
            for (int column = 0; column < columns; column++) {
                cell(row, column).setBlank();
            }
        }
    }

    private void mergeSet(Sheet sheet, String range, String value, CellStyle style) {
        CellRangeAddress address = CellRangeAddress.valueOf(range);
        sheet.addMergedRegion(address);
        for (int rowIndex = address.getFirstRow(); rowIndex <= address.getLastRow(); rowIndex++) {
            for (int column = address.getFirstColumn(); column <= address.getLastColumn(); column++) {
                Cell target = cell(row(sheet, rowIndex), column);
                target.setCellStyle(style);
                if (rowIndex == address.getFirstRow() && column == address.getFirstColumn()) {
                    target.setCellValue(value);
                }
            }
        }
    }

    private void set(Sheet sheet, String reference, String value) {
        CellReference cellReference = new CellReference(reference);
        Cell target = cell(row(sheet, cellReference.getRow()), cellReference.getCol());
        target.setBlank();
        target.setCellValue(defaultValue(value, ""));
    }

    private void setStyled(Sheet sheet, int rowIndex, int column, String value, CellStyle style) {
        Cell target = cell(row(sheet, rowIndex), column);
        target.setBlank();
        target.setCellStyle(style);
        target.setCellValue(defaultValue(value, ""));
    }

    private CellStyle style(Sheet sheet, String reference) {
        CellReference cellReference = new CellReference(reference);
        return cell(row(sheet, cellReference.getRow()), cellReference.getCol()).getCellStyle();
    }

    private Row row(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    private Cell cell(Row row, int column) {
        Cell cell = row.getCell(column);
        return cell == null ? row.createCell(column) : cell;
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultValue(String value, String fallback) {
        return isBlank(value) ? (fallback == null ? "" : fallback) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SchemaField(String path, String type, String description, String length,
                               boolean required, String example, boolean coded) {
    }
}
