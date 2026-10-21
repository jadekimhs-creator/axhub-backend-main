package io.shinhanlife.dat.report.excel;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import io.shinhanlife.dat.report.model.FieldDefinition;
import io.shinhanlife.dat.report.model.ToolSummary;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool의 Input/Output DTO 및 전문 소스(MCI *_I, *_O)를 분석하여 인터페이스 설계서 필드 목록을 생성하는 리졸버.
 */
public class ToolDtoFieldResolver {

    private static final Logger log = LoggerFactory.getLogger(ToolDtoFieldResolver.class);

    private static final Pattern LENGTH_PATTERN = Pattern.compile("length\\s*=\\s*(\\d+)");
    private static final Pattern PATTERN_RANGE = Pattern.compile("\\{(\\d+),(\\d+)\\}");
    private static final Pattern PATTERN_EXACT = Pattern.compile("\\{(\\d+)\\}");

    private static final Pattern MCI_SERVICE_PATTERN = Pattern.compile("\\b(ON[A-Z0-9]{6})(_[IO])?\\b");
    private static final Pattern CLIENT_CONST_PATTERN = Pattern.compile("RECEIVE_SERVICE_ID\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern CLIENT_IF_PATTERN = Pattern.compile("INTERFACE_ID\\s*=\\s*\"([^\"]+)\"");

    // 기본 인터페이스 ID -> MCI 서비스 ID 매핑 테이블
    private static final Map<String, String> STATIC_INTERFACE_TO_SERVICE = Map.of(
            "DATNBSO00004", "ONBSA1030", // 인사검색 목록조회
            "DATNBSO00005", "ONBSZ0460", // 공통인사 단건조회
            "DATNBTO00001", "ONBTI2370", // 모집수수료 공시데이터조회
            "DATNCSO00001", "ONCSC1340", // 개인고객 상세조회
            "CLYMCI00001", "ONCST1341",  // 계약상태조회
            "CLHTTP00005", "ONCSD1340"   // 조직조회
    );

    // 필드 영문명 -> 한글명 공통 사전 (설명 누락 시 fallback)
    private static final Map<String, String> DICTIONARY = Map.ofEntries(
            Map.entry("customerId", "고객ID"),
            Map.entry("customerNumber", "고객번호"),
            Map.entry("referenceDate", "기준일자"),
            Map.entry("resultCode", "결과코드"),
            Map.entry("resultMessage", "결과메시지"),
            Map.entry("name", "고객성명"),
            Map.entry("phoneNumber", "전화번호"),
            Map.entry("email", "이메일"),
            Map.entry("address", "주소"),
            Map.entry("birthDate", "생년월일"),
            Map.entry("registrationDate", "등록일자"),
            Map.entry("grade", "고객등급"),
            Map.entry("companyCode", "회사코드"),
            Map.entry("year", "연도"),
            Map.entry("quarter", "분기"),
            Map.entry("startDate", "시작일자"),
            Map.entry("endDate", "종료일자"),
            Map.entry("prafNo", "인사번호"),
            Map.entry("prafNm", "인사명"),
            Map.entry("prafTypeCd", "인사유형코드"),
            Map.entry("prafEngNm", "인사영문명"),
            Map.entry("blngOgnzNo", "소속조직번호"),
            Map.entry("ognzNo", "조직번호"),
            Map.entry("ognzNm", "조직명"),
            Map.entry("pageNo", "페이지번호"),
            Map.entry("pageDataCc", "페이지데이터건수"),
            Map.entry("totaPageCn", "총페이지수"),
            Map.entry("totaPageDataCc", "총페이지데이터건수"),
            Map.entry("scrPageInfo", "스크롤 페이지 정보"),
            Map.entry("scrImhdNm", "스크롤즉시처리명"),
            Map.entry("scrItva", "스크롤항목값"),
            Map.entry("scrSortValu", "스크롤정렬값"),
            Map.entry("nextDataExtYn", "다음데이터존재여부")
    );

    // 필드 영문명 -> 기본 길이 (길이 정보 누락 시 fallback)
    private static final Map<String, Integer> DEFAULT_LENGTHS = Map.ofEntries(
            Map.entry("customerId", 20),
            Map.entry("customerNumber", 20),
            Map.entry("name", 50),
            Map.entry("phoneNumber", 14),
            Map.entry("email", 50),
            Map.entry("address", 200),
            Map.entry("birthDate", 10),
            Map.entry("registrationDate", 10),
            Map.entry("referenceDate", 10),
            Map.entry("grade", 10),
            Map.entry("resultCode", 10),
            Map.entry("companyCode", 6),
            Map.entry("year", 4),
            Map.entry("quarter", 1),
            Map.entry("startDate", 10),
            Map.entry("endDate", 10)
    );

    public record FieldRow(int level, String description, String parentDescription,
                           String name, String parentName, String dataType, int length) {
    }

    private record ParsedFieldDef(int order, String description, String name,
                                  String type, int length, String nestedType) {
    }

    public record ResolvedDtoResult(String dtoCode, String dtoKoreanName,
                                    String dtoEnglishName, List<FieldRow> fields) {
    }

    /**
     * Tool에 대한 Input 또는 Output DTO를 해석하고 설계서 필드 목록을 생성합니다.
     */
    public ResolvedDtoResult resolveDto(ToolSummary tool, boolean isInput, Path searchRoot, List<FieldDefinition> fallbackFields) {
        String suffix = isInput ? "_I" : "_O";
        List<Path> searchRoots = collectSearchRoots(searchRoot);

        // 1. 대상 서비스 ID 탐색 (우선순위 고려)
        String serviceId = findTargetServiceId(tool, searchRoots);
        String targetDtoName = serviceId != null ? serviceId + suffix : null;

        // 2. 소스 파일에서 DTO 파싱 시도
        if (targetDtoName != null) {
            Path dtoFile = findDtoFile(searchRoots, targetDtoName);
            if (dtoFile != null) {
                try {
                    String source = Files.readString(dtoFile, StandardCharsets.UTF_8);
                    List<FieldRow> parsed = describeFields(source, searchRoots);
                    if (!parsed.isEmpty()) {
                        log.info("Successfully resolved DTO {} with {} fields from {}", targetDtoName, parsed.size(), dtoFile);
                        return new ResolvedDtoResult(targetDtoName, targetDtoName, "", deduplicateFields(parsed));
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse DTO file: {} ({})", dtoFile, e.getMessage());
                }
            }
        }

        // 3. 차선책: UseCase DTO 또는 mappingId 직접 탐색
        List<String> candidateNames = new ArrayList<>();
        if (tool.mappingId() != null && !tool.mappingId().isBlank()) {
            candidateNames.add(tool.mappingId() + suffix);
        }
        String directType = isInput ? tool.requestType() : tool.responseType();
        if (directType != null && !directType.isBlank() && !candidateNames.contains(directType)) {
            candidateNames.add(directType);
        }

        for (String candidate : candidateNames) {
            Path file = findDtoFile(searchRoots, candidate);
            if (file != null) {
                try {
                    String source = Files.readString(file, StandardCharsets.UTF_8);
                    List<FieldRow> parsed = describeFields(source, searchRoots);
                    if (!parsed.isEmpty()) {
                        String codeName = candidate.endsWith(suffix) ? candidate : candidate + suffix;
                        return new ResolvedDtoResult(codeName, codeName, "", deduplicateFields(parsed));
                    }
                } catch (Exception ignored) {}
            }
        }

        // 4. 소스 파일 파싱 실패 시 fallbackFields 활용 (정제 및 중복 제거)
        List<FieldRow> fallbackRows = new ArrayList<>();
        String directionKey = isInput ? "INPUT" : "OUTPUT";
        Set<String> seenFieldNames = new LinkedHashSet<>();

        if (fallbackFields != null) {
            for (FieldDefinition f : fallbackFields) {
                if (directionKey.equalsIgnoreCase(f.direction())) {
                    String fieldName = f.fieldName();
                    if (!seenFieldNames.add(fieldName)) {
                        continue;
                    }
                    int len = 0;
                    if (f.constraints() != null) {
                        Matcher lm = LENGTH_PATTERN.matcher(f.constraints());
                        if (lm.find()) {
                            try { len = Integer.parseInt(lm.group(1)); } catch (Exception ignored) {}
                        }
                    }
                    if (len == 0 && DEFAULT_LENGTHS.containsKey(fieldName)) {
                        len = DEFAULT_LENGTHS.get(fieldName);
                    }
                    String desc = cleanDescription(f.description(), fieldName);
                    fallbackRows.add(new FieldRow(
                            1,
                            desc,
                            "",
                            fieldName,
                            "",
                            f.dataType() != null ? f.dataType() : "String",
                            len
                    ));
                }
            }
        }

        String finalCode = targetDtoName != null ? targetDtoName
                : (tool.mappingId() != null && !tool.mappingId().isBlank() ? tool.mappingId() + suffix : directType);
        return new ResolvedDtoResult(
                finalCode != null ? finalCode : (isInput ? "REQUEST_DTO" : "RESPONSE_DTO"),
                finalCode != null ? finalCode : (isInput ? "요청 DTO" : "응답 DTO"),
                "",
                fallbackRows
        );
    }

    private List<FieldRow> deduplicateFields(List<FieldRow> list) {
        Map<String, FieldRow> map = new LinkedHashMap<>();
        for (FieldRow r : list) {
            String key = r.level() + ":" + r.name() + ":" + r.parentName();
            map.putIfAbsent(key, r);
        }
        return new ArrayList<>(map.values());
    }

    /**
     * Tool에 대응되는 MCI 서비스 ID(예: ONBSA1030, ONCST1341 등)를 식별합니다.
     */
    public String findTargetServiceId(ToolSummary tool, List<Path> searchRoots) {
        String mappingId = tool.mappingId() != null ? tool.mappingId().trim() : "";

        // 1. mappingId 자체가 이미 ONBSA1030 등의 MCI 서비스 ID 형태인 경우
        if (mappingId.matches("^ON[A-Z0-9]{6}$")) {
            return mappingId;
        }

        // 2. 툴 제목, 설명, 이름에 인사/직원/employee 관련 단어가 있거나 mappingId가 DATNBSO00004인 경우 ONBSA1030 우선
        if ("DATNBSO00004".equalsIgnoreCase(mappingId)
                || tool.name().contains("employee")
                || tool.name().contains("praf")
                || (tool.title() != null && (tool.title().contains("인사") || tool.title().contains("직원")))
                || (tool.description() != null && (tool.description().contains("인사검색") || tool.description().contains("ONBSA1030")))) {
            return "ONBSA1030";
        }

        // 3. 정적 인터페이스 ID 매핑 확인
        if (STATIC_INTERFACE_TO_SERVICE.containsKey(mappingId)) {
            return STATIC_INTERFACE_TO_SERVICE.get(mappingId);
        }

        // 4. 소스 디렉토리 내 Mci*Client.java 파일들을 스캔하여 INTERFACE_ID와 RECEIVE_SERVICE_ID 매핑 검색
        Map<String, String> scannedMappings = scanClientMappings(searchRoots);
        if (scannedMappings.containsKey(mappingId)) {
            return scannedMappings.get(mappingId);
        }

        // 5. UseCase 및 UseCaseImpl 소스에서 직접 참조하는 ONxxxxxx DTO 검색
        String fromUseCase = scanMciServiceFromUseCase(tool, searchRoots);
        if (fromUseCase != null) {
            return fromUseCase;
        }

        return !mappingId.isBlank() ? mappingId : null;
    }

    private List<Path> collectSearchRoots(Path base) {
        Set<Path> roots = new LinkedHashSet<>();
        if (base != null && Files.isDirectory(base)) {
            roots.add(base.toAbsolutePath().normalize());
            Path parent = base.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                roots.add(parent);
                addIfDir(roots, parent.resolve("dat-was-datps"));
                addIfDir(roots, parent.resolve("dat-lib-datmt"));
                addIfDir(roots, parent.resolve("workspace/dat-was-datps"));
                addIfDir(roots, parent.resolve("workspace/dat-lib-datmt"));
            }
        }
        addIfDir(roots, Path.of("C:/eGovFrameDev-4.3.1-64bit/workspace/dat-was-datps"));
        addIfDir(roots, Path.of("C:/eGovFrameDev-4.3.1-64bit/workspace/dat-lib-datmt"));
        addIfDir(roots, Path.of("C:/eGovFrameDev-4.3.1-64bit/workspace-egov/axhub-backend-main"));
        addIfDir(roots, Path.of("C:/eGovFrameDev-4.3.1-64bit/workspace-egov/dat-was-datmt"));
        return List.copyOf(roots);
    }

    private void addIfDir(Set<Path> set, Path path) {
        Path norm = path.toAbsolutePath().normalize();
        if (Files.isDirectory(norm)) {
            set.add(norm);
        }
    }

    private Map<String, String> scanClientMappings(List<Path> searchRoots) {
        Map<String, String> mappings = new HashMap<>(STATIC_INTERFACE_TO_SERVICE);
        for (Path root : searchRoots) {
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith("Client.java"))
                        .forEach(clientFile -> {
                            try {
                                String content = Files.readString(clientFile, StandardCharsets.UTF_8);
                                Matcher ifM = CLIENT_IF_PATTERN.matcher(content);
                                Matcher srvM = CLIENT_CONST_PATTERN.matcher(content);
                                if (ifM.find() && srvM.find()) {
                                    mappings.put(ifM.group(1).trim(), srvM.group(1).trim());
                                }
                            } catch (Exception ignored) {}
                        });
            } catch (Exception ignored) {}
        }
        return mappings;
    }

    private String scanMciServiceFromUseCase(ToolSummary tool, List<Path> searchRoots) {
        String useCaseName = tool.useCaseClass();
        if (useCaseName == null || useCaseName.isBlank()) return null;

        List<String> targetFileNames = List.of(useCaseName + ".java", useCaseName + "Impl.java");
        for (Path root : searchRoots) {
            for (String targetFileName : targetFileNames) {
                Path found = findFileByName(root, targetFileName);
                if (found != null) {
                    try {
                        String content = Files.readString(found, StandardCharsets.UTF_8);
                        Matcher m = MCI_SERVICE_PATTERN.matcher(content);
                        while (m.find()) {
                            String match = m.group(1);
                            if (match.startsWith("ON") && match.length() == 9) {
                                return match;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }

    private Path findDtoFile(List<Path> searchRoots, String dtoName) {
        String targetFileName = dtoName + ".java";
        for (Path root : searchRoots) {
            Path found = findFileByName(root, targetFileName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private Path findFileByName(Path root, String fileName) {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(fileName))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * JavaParser AST를 활용하여 DTO 소스를 계층 구조와 함께 정밀 분석합니다.
     */
    private List<FieldRow> describeFields(String source, List<Path> searchRoots) {
        List<FieldRow> output = new ArrayList<>();
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            TypeDeclaration<?> primaryType = cu.getPrimaryType().orElse(null);
            if (primaryType == null && !cu.getTypes().isEmpty()) {
                primaryType = cu.getType(0);
            }
            if (primaryType != null) {
                appendFieldsFromType(primaryType, 1, "", "", output, searchRoots, cu);
                return output;
            }
        } catch (Exception e) {
            log.warn("JavaParser AST parse failed: {}. Falling back to regex scanner.", e.getMessage());
        }

        // AST 파싱 실패 시 폴백
        return output;
    }

    private void appendFieldsFromType(TypeDeclaration<?> type, int level, String parentName, String parentDescription,
                                      List<FieldRow> output, List<Path> searchRoots, CompilationUnit cu) {
        List<ParsedFieldDef> parsedFields = new ArrayList<>();
        int autoOrder = 1;

        for (FieldDeclaration fieldDecl : type.getFields()) {
            if (fieldDecl.isStatic()) continue;

            for (var variable : fieldDecl.getVariables()) {
                String fieldName = variable.getNameAsString();
                if ("serialVersionUID".equals(fieldName)) continue;

                String rawType = variable.getTypeAsString();
                int order = autoOrder++;
                int length = 0;
                String desc = null;
                String typeAttr = null;
                String patternRegexp = null;

                for (AnnotationExpr anno : fieldDecl.getAnnotations()) {
                    String annoName = anno.getName().getIdentifier();
                    if ("GlowTrgmField".equals(annoName) || "GlowMciFieldInfo".equals(annoName)) {
                        order = getAnnotationInt(anno, "order", order);
                        length = getAnnotationInt(anno, "length", length);
                        desc = getAnnotationString(anno, "description", desc);
                        typeAttr = getAnnotationString(anno, "type", typeAttr);
                    } else if ("Schema".equals(annoName)) {
                        String sDesc = getAnnotationString(anno, "description", null);
                        if (sDesc != null && desc == null) {
                            desc = sDesc;
                        }
                    } else if ("Pattern".equals(annoName)) {
                        patternRegexp = getAnnotationString(anno, "regexp", null);
                    }
                }

                if (length == 0 && patternRegexp != null) {
                    length = extractLengthFromRegexp(patternRegexp);
                }
                if (length == 0) {
                    length = extractLengthFromFallback(fieldName, rawType);
                }

                String cleanDesc = cleanDescription(desc, fieldName);
                String dataType = typeAttr != null ? typeAttr : simpleType(rawType);
                String nestedType = extractNestedType(rawType);
                if (nestedType != null && typeAttr == null) {
                    dataType = "gm";
                }

                parsedFields.add(new ParsedFieldDef(order, cleanDesc, fieldName, dataType, length, nestedType));
            }
        }

        parsedFields.sort(Comparator.comparingInt(ParsedFieldDef::order));

        for (ParsedFieldDef field : parsedFields) {
            output.add(new FieldRow(level, field.description(), parentDescription, field.name(),
                    parentName, field.type(), field.length()));

            if (field.nestedType() != null && !field.nestedType().equals(type.getNameAsString())) {
                String nestedName = field.nestedType();
                if ("PageInfo".equals(nestedName)) {
                    appendPageInfoFields(level + 1, field.name(), field.description(), output);
                } else if ("ScrPageInfo".equals(nestedName)) {
                    appendScrPageInfoFields(level + 1, field.name(), field.description(), output);
                } else {
                    // 동일 파일 내 중첩 클래스 검색
                    TypeDeclaration<?> innerType = type.getMembers().stream()
                            .filter(BodyDeclaration::isTypeDeclaration)
                            .map(BodyDeclaration::asTypeDeclaration)
                            .filter(t -> t.getNameAsString().equals(nestedName))
                            .findFirst()
                            .orElse(null);

                    if (innerType == null && cu != null) {
                        innerType = cu.getTypes().stream()
                                .filter(t -> t.getNameAsString().equals(nestedName))
                                .findFirst()
                                .orElse(null);
                    }

                    if (innerType != null) {
                        appendFieldsFromType(innerType, level + 1, field.name(), field.description(), output, searchRoots, cu);
                    } else {
                        // 외부 파일 검색
                        Path extFile = findDtoFile(searchRoots, nestedName);
                        if (extFile != null) {
                            try {
                                CompilationUnit extCu = StaticJavaParser.parse(extFile);
                                TypeDeclaration<?> extType = extCu.getPrimaryType().orElse(null);
                                if (extType != null) {
                                    appendFieldsFromType(extType, level + 1, field.name(), field.description(), output, searchRoots, extCu);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
    }

    private void appendPageInfoFields(int level, String parentName, String parentDescription, List<FieldRow> output) {
        output.add(new FieldRow(level, "페이지번호", parentDescription, "pageNo", parentName, "int", 5));
        output.add(new FieldRow(level, "페이지데이터건수", parentDescription, "pageDataCc", parentName, "int", 5));
        output.add(new FieldRow(level, "총페이지수", parentDescription, "totaPageCn", parentName, "int", 10));
        output.add(new FieldRow(level, "총페이지데이터건수", parentDescription, "totaPageDataCc", parentName, "int", 10));
    }

    private void appendScrPageInfoFields(int level, String parentName, String parentDescription, List<FieldRow> output) {
        output.add(new FieldRow(level, "페이지데이터건수", parentDescription, "pageDataCc", parentName, "int", 5));
        output.add(new FieldRow(level, "스크롤즉시처리명", parentDescription, "scrImhdNm", parentName, "String", 100));
        output.add(new FieldRow(level, "스크롤항목값", parentDescription, "scrItva", parentName, "String", 100));
        output.add(new FieldRow(level, "스크롤정렬값", parentDescription, "scrSortValu", parentName, "String", 100));
        output.add(new FieldRow(level, "다음데이터존재여부", parentDescription, "nextDataExtYn", parentName, "String", 1));
    }

    private String cleanDescription(String rawDesc, String fieldName) {
        if (rawDesc == null || rawDesc.isBlank()) {
            return DICTIONARY.getOrDefault(fieldName, fieldName);
        }
        // (형식: ...), (예시: ...), (선택적 ...) 등 괄호 부가설명 정제
        String cleaned = rawDesc.replaceAll("\\s*\\([^)]*\\).*", "").trim();
        if (cleaned.isBlank() || cleaned.equalsIgnoreCase(fieldName)) {
            return DICTIONARY.getOrDefault(fieldName, fieldName);
        }
        return cleaned;
    }

    private int extractLengthFromRegexp(String regexp) {
        if (regexp == null || regexp.isBlank()) return 0;
        Matcher rm = PATTERN_RANGE.matcher(regexp);
        if (rm.find()) {
            try { return Integer.parseInt(rm.group(2).trim()); } catch (Exception ignored) {}
        }
        Matcher em = PATTERN_EXACT.matcher(regexp);
        if (em.find()) {
            try { return Integer.parseInt(em.group(1).trim()); } catch (Exception ignored) {}
        }
        if (regexp.contains("d{4}") || regexp.contains("0-9]{4}")) {
            return 10;
        }
        return 0;
    }

    private int extractLengthFromFallback(String fieldName, String rawType) {
        if (DEFAULT_LENGTHS.containsKey(fieldName)) {
            return DEFAULT_LENGTHS.get(fieldName);
        }
        if (fieldName.toLowerCase().contains("date")) {
            return 10;
        }
        if ("int".equals(rawType) || "Integer".equals(rawType)) return 10;
        if ("long".equals(rawType) || "Long".equals(rawType)) return 18;
        return 0;
    }

    private String simpleType(String rawType) {
        if (rawType.startsWith("List<")) {
            return "gm";
        }
        int lastDot = rawType.lastIndexOf('.');
        return lastDot != -1 ? rawType.substring(lastDot + 1) : rawType;
    }

    private String extractNestedType(String rawType) {
        if (rawType.startsWith("List<") && rawType.endsWith(">")) {
            String inner = rawType.substring(5, rawType.length() - 1).trim();
            int lastDot = inner.lastIndexOf('.');
            return lastDot != -1 ? inner.substring(lastDot + 1) : inner;
        }
        if (!isPrimitiveOrStandard(rawType)) {
            int lastDot = rawType.lastIndexOf('.');
            return lastDot != -1 ? rawType.substring(lastDot + 1) : rawType;
        }
        return null;
    }

    private boolean isPrimitiveOrStandard(String type) {
        return switch (type) {
            case "String", "int", "Integer", "long", "Long", "double", "Double",
                 "float", "Float", "boolean", "Boolean", "byte", "Byte",
                 "short", "Short", "char", "Character", "BigDecimal",
                 "LocalDate", "LocalDateTime", "Date" -> true;
            default -> false;
        };
    }

    private Optional<Expression> getAnnotationValue(AnnotationExpr annotation, String key) {
        if (annotation instanceof NormalAnnotationExpr normal) {
            return normal.getPairs().stream()
                    .filter(p -> p.getNameAsString().equals(key))
                    .map(MemberValuePair::getValue)
                    .findFirst();
        }
        if (annotation instanceof SingleMemberAnnotationExpr single && "value".equals(key)) {
            return Optional.of(single.getMemberValue());
        }
        return Optional.empty();
    }

    private String getAnnotationString(AnnotationExpr annotation, String key, String fallback) {
        return getAnnotationValue(annotation, key)
                .filter(StringLiteralExpr.class::isInstance)
                .map(StringLiteralExpr.class::cast)
                .map(StringLiteralExpr::asString)
                .orElse(fallback);
    }

    private int getAnnotationInt(AnnotationExpr annotation, String key, int fallback) {
        return getAnnotationValue(annotation, key)
                .filter(IntegerLiteralExpr.class::isInstance)
                .map(IntegerLiteralExpr.class::cast)
                .map(IntegerLiteralExpr::asNumber)
                .map(Number::intValue)
                .orElse(fallback);
    }
}
