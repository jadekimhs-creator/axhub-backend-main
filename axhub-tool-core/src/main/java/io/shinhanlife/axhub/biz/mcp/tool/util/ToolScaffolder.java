package io.shinhanlife.axhub.biz.mcp.tool.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
 *  - 컴파일: javac -encoding UTF-8 axhub-tool-core/src/main/java/io/shinhanlife/axhub/biz/mcp/tool/util/ToolScaffolder.java
 *  - 실행: java -cp axhub-tool-core/src/main/java io.shinhanlife.axhub.biz.mcp.tool.util.ToolScaffolder [이름] [ID] "[설명]" "[그룹]" "[통신방식]" "[모듈명]"
 */
/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.util
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

    private static final String BASE_PACKAGE = "io.shinhanlife.axhub.biz.mcp.tool";
    private static final String BASE_PACKAGE_PATH = "src/main/java/io/shinhanlife/axhub/biz/mcp/tool";

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=========================================");
        System.out.println("   MCP Tool Scaffolder (Java CLI)   ");
        System.out.println("=========================================\n");

        String baseName = getOrAsk(args, 0, scanner, "1. 생성할 Tool의 기본 이름 (예: ExchangeRate) [영문 PascalCase]: ");
        String interfaceId = getOrAsk(args, 1, scanner, "2. 레거시 API 인터페이스 ID (예: EXCH_001): ");
        String description = getOrAsk(args, 2, scanner, "3. Tool 기능 설명 (예: 환율 조회): ");
        String group = getOrAsk(args, 3, scanner, "4. Tool 소속 그룹 (예: NOTIFICATION, CLAIM, POLICY, HR, CONTRACT, CUSTOMER 등): ");
        if (group.isEmpty()) group = "COMMON";
        String routingType = getOrAsk(args, 4, scanner, "5. 통신 프로토콜 (예: HTTP, TCP, MCI, EAI): ");
        if (routingType.trim().isEmpty()) {
            routingType = "HTTP";
        }
        String moduleName = getOrAsk(args, 5, scanner, "6. 코드를 생성할 모듈 (기본: axhub-tool-other): ");
        if (moduleName.trim().isEmpty()) {
            moduleName = "axhub-tool-other";
        }

        scaffold(baseName, interfaceId, description, group, routingType, moduleName);
    }

    private static String getOrAsk(String[] args, int index, Scanner scanner, String prompt) {
        if (args.length > index) {
            return args[index];
        }
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static void scaffold(String baseName, String interfaceId, String description, String group, String routingType, String moduleName) throws IOException {
        Path serviceDir = Paths.get(moduleName, BASE_PACKAGE_PATH, "service");
        Path dtoDir = Paths.get(moduleName, BASE_PACKAGE_PATH, "dto");

        Files.createDirectories(serviceDir);
        Files.createDirectories(dtoDir);

        // Generate Req DTO
        String reqContent = """
            package %s.dto;

            import com.fasterxml.jackson.annotation.JsonInclude;
            import lombok.Data;

            /**
             * @package %s.dto
             * @className %sReq
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
            @Data
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public class %sReq {
                // TODO: Add request fields here
            }
            """.formatted(BASE_PACKAGE, BASE_PACKAGE, baseName, baseName);
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
            @Data
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public class %sRes {
                private String status;
                private String message;
                // TODO: Add response fields here
            }
            """.formatted(BASE_PACKAGE, BASE_PACKAGE, baseName, baseName);
        Files.writeString(dtoDir.resolve(baseName + "Res.java"), resContent);

        String toolName = baseName.toLowerCase();

        // Generate Service
        String serviceContent = """
            package %s.service;

            import %s.annotation.McpFunction;
            import %s.annotation.McpTool;
            import %s.dto.%sReq;
            import %s.dto.%sRes;
            import org.springframework.stereotype.Service;

            /**
             * @package %s.service
             * @className %sService
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
            @Service
            @McpTool(
                routingType = "%s",
                group = "%s"
            )
            public class %sService extends AbstractMcpToolService {

                @McpFunction(
                    name = "%s",
                    description = "%s",
                    prompt = "%s",
                    mappingId = "%s"
                )
                public Object execute(%sReq req) {
                    return executeLegacy("%s", "%s", req);
                }
            }
            """.formatted(
                BASE_PACKAGE, 
                BASE_PACKAGE, 
                BASE_PACKAGE, 
                BASE_PACKAGE, baseName, 
                BASE_PACKAGE, baseName,
                BASE_PACKAGE, baseName,
                routingType, 
                group, 
                baseName, 
                toolName,
                description, 
                description + " 해줘.", 
                interfaceId, 
                baseName, 
                routingType, 
                interfaceId
            );
        
        Files.writeString(serviceDir.resolve(baseName + "Service.java"), serviceContent);

        // Append to YAML if exists
        Path yamlPath = Paths.get(moduleName, "src/main/resources", "application-local.yml");
        if (Files.exists(yamlPath)) {
            String yamlContent = Files.readString(yamlPath);
            if (yamlContent.contains("functions:")) {
                String newFunctionYaml = """
                    
                        %s:
                          description: "%s"
                          prompt: "%s"
                          mappingId: "%s"
                """.formatted(toolName, description, description + " 해줘.", interfaceId);
                yamlContent = yamlContent.replaceFirst("functions:", "functions:" + newFunctionYaml);
                Files.writeString(yamlPath, yamlContent);
                System.out.println("[YAML] " + yamlPath + " (함수 설정 자동 등록됨)");
            }
        }

        System.out.println("\n=========================================");
        System.out.println(" Scaffolding Complete!");
        System.out.println("=========================================");
        System.out.println("[Service] " + serviceDir.resolve(baseName + "Service.java"));
        System.out.println("[Req DTO] " + dtoDir.resolve(baseName + "Req.java"));
        System.out.println("[Res DTO] " + dtoDir.resolve(baseName + "Res.java"));
        System.out.println("\n Tip: " + interfaceId + " 목업 데이터를 mock-responses.json에 추가하세요.");
    }
}