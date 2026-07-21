package io.shinhanlife.dap.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * MCP Tool 코드를 자동 생성(Scaffolding)하는 유틸리티 클래스
 * 
 * [실행 방법]
 * 방법 1. IDE(IntelliJ 등)에서 직접 실행 (대화형 모드 추천 ⭐)
 *  - 이 클래스(ToolScaffolder.java)를 열고 main 메서드를 직접 실행(Run)합니다.
 *  - 콘솔 창에 뜨는 질문에 차례대로 값을 입력하기만 하면 파일이 생성됩니다.
 * 
 * 방법 2. 커맨드라인(터미널)에서 실행 (명령어 기반)
 *  - 컴파일: javac -encoding UTF-8 dap-tool-core/src/main/java/io/shinhanlife/dap/mcc/util/ToolScaffolder.java
 *  - 실행: java -cp dap-tool-core/src/main/java io.shinhanlife.dap.mcc.util.ToolScaffolder [이름] [ID] "[설명]" "[그룹]" "[통신방식]" "[모듈명]"
 */
/**
 * @package io.shinhanlife.dap.mcc.util
 * @className ToolScaffolder
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
public class ToolScaffolder {

    private static final String BASE_PACKAGE = "io.shinhanlife.dap.mcc";
    private static final String BASE_PACKAGE_PATH = "src/main/java/io/shinhanlife/dap/mcc";

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=========================================");
        System.out.println("   MCP Tool Scaffolder (Java CLI)   ");
        System.out.println("=========================================\n");

        String baseName = getOrAsk(args, 0, scanner, "1. 생성할 Tool의 기본 이름 (예: ExchangeRate) [영문 PascalCase]: ");
        String interfaceId = getOrAsk(args, 1, scanner, "2. 레거시 API 인터페이스 ID (예: EXCH_001): ");
        String description = getOrAsk(args, 2, scanner, "3. Tool 기능 설명 (예: 환율 조회): ");
        String group = getOrAsk(args, 3, scanner, "4. Tool 소속 그룹 (예: SAMPLE, NOTIFICATION, CLAIM, POLICY, HR, CONTRACT, CUSTOMER 등): ");
        if (group.isEmpty()) group = "COMMON";
        String routingType = getOrAsk(args, 4, scanner, "5. 통신 프로토콜 (예: HTTP, TCP, MCI, EAI): ");
        if (routingType.trim().isEmpty()) {
            routingType = "HTTP";
        }
        String moduleName = getOrAsk(args, 5, scanner, "6. 코드를 생성할 모듈 (기본: dap-tool-other): ");
        if (moduleName.trim().isEmpty()) {
            moduleName = "dap-tool-other";
        }
        
        String defaultAuthor = System.getProperty("user.name");
        String defaultDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

        String author = getOrAsk(args, 6, scanner, "7. 작성자 (엔터 입력 시 '" + defaultAuthor + "'): ");
        if (author.trim().isEmpty()) author = defaultAuthor;
        String createDate = getOrAsk(args, 7, scanner, "8. 작성일 (엔터 입력 시 '" + defaultDate + "'): ");
        if (createDate.trim().isEmpty()) createDate = defaultDate;

        String result = scaffold(baseName, interfaceId, description, group, routingType, moduleName, author, createDate, true);
        System.out.println(result);
    }

    private static String getOrAsk(String[] args, int index, Scanner scanner, String prompt) {
        if (args.length > index) {
            return args[index];
        }
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static String scaffold(String baseName, String interfaceId, String description, String group, String routingType, String moduleName, String author, String createDate, boolean register) throws IOException {
        baseName = toPascalCase(baseName);
        String envSourceDir = System.getenv("AXHUB_SOURCE_DIR");
        Path rootDir = envSourceDir != null ? Paths.get(envSourceDir) : Paths.get(".");
        
        Path serviceDir = rootDir.resolve(Paths.get(moduleName, BASE_PACKAGE_PATH, "service"));
        Path dtoDir = rootDir.resolve(Paths.get(moduleName, BASE_PACKAGE_PATH, "dto"));

        String shortName = moduleName.replace("dap-tool-", "").replace("-", "");
        Path legacyDtoDir = rootDir.resolve(Paths.get(moduleName, BASE_PACKAGE_PATH, shortName, "dto"));
        Path converterDir = rootDir.resolve(Paths.get(moduleName, BASE_PACKAGE_PATH, shortName, "converter"));

        Files.createDirectories(serviceDir);
        Files.createDirectories(dtoDir);
        Files.createDirectories(legacyDtoDir);
        Files.createDirectories(converterDir);

        StringBuilder log = new StringBuilder();

        // Generate Req DTO
        String reqContent = """
            package %s.dto;

            import com.fasterxml.jackson.annotation.JsonInclude;
            import lombok.Data;

            /**
             * @package %s.dto
             * @className %sReq
             * @description AX HUB 시스템 처리 클래스
             * @author %s
             * @create %s
             * <pre>
             * ---------- 개정이력 ----------
             * 수정일      수정자    수정내용
             * ---------- -------- ---------------------------
             * %s  %s    최초생성
             * 
             * </pre>
             */
            @Data
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public class %sReq {
                // TODO: Add request fields here
            }
            """.formatted(BASE_PACKAGE, BASE_PACKAGE, baseName, author, createDate, createDate, author, baseName);
        Files.writeString(dtoDir.resolve(baseName + "Req.java"), reqContent);

        // Generate Res DTO
        String resContent = """
            package %s.dto;

            import com.fasterxml.jackson.annotation.JsonInclude;
            import lombok.Data;

            /**
             * @package %s.dto
             * @className %sRes
             * @description AX HUB 시스템 처리 클래스
             * @author %s
             * @create %s
             * <pre>
             * ---------- 개정이력 ----------
             * 수정일      수정자    수정내용
             * ---------- -------- ---------------------------
             * %s  %s    최초생성
             * 
             * </pre>
             */
            @Data
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public class %sRes {
                private String status;
                private String message;
                // TODO: Add response fields here
            }
            """.formatted(BASE_PACKAGE, BASE_PACKAGE, baseName, author, createDate, createDate, author, baseName);
        Files.writeString(dtoDir.resolve(baseName + "Res.java"), resContent);

        String toolName = baseName.isEmpty() ? baseName : Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1);

        // Generate Service
        String serviceContent = """
            package %s.service;

            import %s.annotation.McpFunction;
            import %s.annotation.McpTool;
            import %s.dto.%sReq;
            import %s.dto.%sRes;
            import %s.%s.converter.%sLegacyConverter;
            import org.springframework.stereotype.Service;
            import org.mapstruct.factory.Mappers;

            /**
             * @package %s.service
             * @className %sService
             * @description AX HUB 시스템 처리 클래스
             * @author %s
             * @create %s
             * <pre>
             * ---------- 개정이력 ----------
             * 수정일      수정자    수정내용
             * ---------- -------- ---------------------------
             * %s  %s    최초생성
             * 
             * </pre>
             */
            @Service
            @McpTool(
                routingType = "%s",
                categoryKey = "%s"
            )
            public class %sService extends AbstractMcpToolService {

                private final %sLegacyConverter converter = Mappers.getMapper(%sLegacyConverter.class);

                @McpFunction(
                    displayName = "%s 툴",
                    name = "%s",
                    description = "%s",
                    prompt = "%s",
                    mappingId = "%s",
                    register = %s,
                    requiresApproval = false
                )
                public Object execute(%sReq req) {
                    // %sLegacyReq legacyReq = converter.toLegacyReq(req);
                    return executeLegacy("%s", "%s", req); // Or pass legacyReq
                }
            }
            """.formatted(
                BASE_PACKAGE,
                BASE_PACKAGE,
                BASE_PACKAGE,
                BASE_PACKAGE, baseName,
                BASE_PACKAGE, baseName,
                BASE_PACKAGE, shortName, baseName,
                BASE_PACKAGE,
                baseName,
                author,
                createDate,
                createDate, author,
                routingType, group.toLowerCase(),
                baseName,
                baseName, baseName,
                baseName, toolName, description, description + " 해줘.", interfaceId, register,
                baseName,
                baseName,
                routingType, interfaceId
            );
        
        Files.writeString(serviceDir.resolve(baseName + "Service.java"), serviceContent);



        // Generate Legacy Req DTO
        String legacyReqContent = """
            package %s.%s.dto;

            import lombok.Data;

            /**
             * @package %s.%s.dto
             * @className %sLegacyReq
             * @description AX HUB 시스템 처리 클래스
             * @author %s
             * @create %s
             * <pre>
             * ---------- 개정이력 ----------
             * 수정일      수정자    수정내용
             * ---------- -------- ---------------------------
             * %s  %s    최초생성
             * 
             * </pre>
             */
            @Data
            public class %sLegacyReq {
                // TODO: Add legacy request fields here
            }
            """.formatted(BASE_PACKAGE, shortName, BASE_PACKAGE, shortName, baseName, author, createDate, createDate, author, baseName);
        Files.writeString(legacyDtoDir.resolve(baseName + "LegacyReq.java"), legacyReqContent);

        // Generate Legacy Res DTO
        String legacyResContent = """
            package %s.%s.dto;

            import lombok.Data;

            /**
             * @package %s.%s.dto
             * @className %sLegacyRes
             * @description AX HUB 시스템 처리 클래스
             * @author %s
             * @create %s
             * <pre>
             * ---------- 개정이력 ----------
             * 수정일      수정자    수정내용
             * ---------- -------- ---------------------------
             * %s  %s    최초생성
             * 
             * </pre>
             */
            @Data
            public class %sLegacyRes {
                // TODO: Add legacy response fields here
            }
            """.formatted(BASE_PACKAGE, shortName, BASE_PACKAGE, shortName, baseName, author, createDate, createDate, author, baseName);
        Files.writeString(legacyDtoDir.resolve(baseName + "LegacyRes.java"), legacyResContent);

        // Generate Legacy Converter
        String converterContent = """
            package %s.%s.converter;

            import %s.dto.%sReq;
            import %s.dto.%sRes;
            import %s.%s.dto.%sLegacyReq;
            import %s.%s.dto.%sLegacyRes;
            import org.mapstruct.Mapper;
            import org.mapstruct.Mapping;
            import org.mapstruct.factory.Mappers;

            /**
             * @package %s.%s.converter
             * @className %sLegacyConverter
             * @description AX HUB 시스템 처리 클래스
             * @author %s
             * @create %s
             * <pre>
             * ---------- 개정이력 ----------
             * 수정일      수정자    수정내용
             * ---------- -------- ---------------------------
             * %s  %s    최초생성
             * 
             * </pre>
             */
            @Mapper(componentModel = "spring")
            public interface %sLegacyConverter {

                // @Mapping(source = "sourceField", target = "targetField")
                %sLegacyReq toLegacyReq(%sReq req);
                
                %sRes toRes(%sLegacyRes legacyRes);
            }
            """.formatted(
                BASE_PACKAGE, shortName,
                BASE_PACKAGE, baseName,
                BASE_PACKAGE, baseName,
                BASE_PACKAGE, shortName, baseName,
                BASE_PACKAGE, shortName, baseName,
                BASE_PACKAGE, shortName, baseName, author, createDate, createDate, author,
                baseName, baseName, baseName, baseName, baseName, baseName
            );
        Files.writeString(converterDir.resolve(baseName + "LegacyConverter.java"), converterContent);

        log.append("\n=========================================\n");
        log.append(" Scaffolding Complete!\n");
        log.append("=========================================\n");
        log.append("[Service] ").append(serviceDir.resolve(baseName + "Service.java")).append("\n");
        log.append("[Req DTO] ").append(dtoDir.resolve(baseName + "Req.java")).append("\n");
        log.append("[Res DTO] ").append(dtoDir.resolve(baseName + "Res.java")).append("\n");
        log.append("[Legacy Req DTO] ").append(legacyDtoDir.resolve(baseName + "LegacyReq.java")).append("\n");
        log.append("[Legacy Res DTO] ").append(legacyDtoDir.resolve(baseName + "LegacyRes.java")).append("\n");
        log.append("[Legacy Converter] ").append(converterDir.resolve(baseName + "LegacyConverter.java")).append("\n");
        log.append("\n Tip: ").append(interfaceId).append(" 목업 데이터를 mock-responses.json에 추가하세요.\n");
        
        return log.toString();
    }

    private static String toPascalCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : str.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        if (result.length() > 0) {
            result.setCharAt(0, Character.toUpperCase(result.charAt(0)));
        }
        return result.toString();
    }
}