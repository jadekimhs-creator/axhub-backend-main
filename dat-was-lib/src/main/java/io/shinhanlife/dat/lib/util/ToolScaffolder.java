package io.shinhanlife.dat.lib.util;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * MCP Tool 코드를 자동 생성(Scaffolding)하는 유틸리티 클래스입니다.
 *
 * [실행 방법]
 * 방법 1. IDE(IntelliJ 등)에서 직접 실행
 *  - ToolScaffolder.java의 main 메서드를 실행합니다.
 *  - 콘솔 질문에 차례대로 값을 입력하면 파일이 생성됩니다.
 *
 * 방법 2. 명령줄에서 실행
 *  - 컴파일: javac -encoding UTF-8 dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java
 *  - 실행: java -cp dat-was-lib/src/main/java io.shinhanlife.dat.lib.util.ToolScaffolder [이름] [ID] "[설명]" "[그룹]" "[통신방식]" "[모듈명]"
 */
/**
 * @package io.shinhanlife.dat.lib.util
 * @className ToolScaffolder
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일       수정자     수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 *
 * </pre>
 */
public class ToolScaffolder {

    private static final String BASE_PACKAGE = "io.shinhanlife.dat.mcc";
    private static final String BASE_PACKAGE_PATH = "src/main/java/io/shinhanlife/dat/mcc";

    public record FieldDefinition(String name, String type, String description, List<String> examples, String pattern, boolean required,
                                  List<String> enumValues, String itemType, List<FieldDefinition> itemFields) {
        public FieldDefinition(String name, String type, String description, List<String> examples, String pattern, boolean required) {
            this(name, type, description, examples, pattern, required, List.of(), null, List.of());
        }
    }

    public record ToolMethodDefinition(String baseName, String methodName, String interfaceId,
                                       String title, String description, String group, String routingType,
                                       boolean register, String clientSystemCode, String httpApiName,
                                       List<FieldDefinition> inputFields, List<FieldDefinition> outputFields,
                                       ToolDefinitionOptions definitionOptions) {
    }

    public record ToolDefinitionOptions(
            String functionDescription,
            String whenToUse,
            String whenNotToUse,
            String ioLimits,
            String displayDescription,
            List<String> exampleQueries,
            List<String> tags,
            String ownerOrg,
            Long timeoutMillis,
            Integer retryMaxAttempts) {

        public ToolDefinitionOptions(String functionDescription, String whenToUse, String whenNotToUse,
                                     String ioLimits, String displayDescription, List<String> exampleQueries,
                                     List<String> tags, String ownerOrg) {
            this(functionDescription, whenToUse, whenNotToUse, ioLimits, displayDescription, exampleQueries, tags,
                    ownerOrg, null, null);
        }

        public long timeoutMillisOrDefault() {
            return timeoutMillis != null && timeoutMillis > 0 ? timeoutMillis : 5000L;
        }

        public int retryMaxAttemptsOrDefault() {
            return retryMaxAttempts != null && retryMaxAttempts > 0 ? retryMaxAttempts : 3;
        }
    }

    public static String scaffoldUseCase(String useCaseName, String moduleName, String author,
                                         String createDate, List<ToolMethodDefinition> tools) throws IOException {
        if (tools == null || tools.isEmpty()) {
            throw new IllegalArgumentException("At least one Tool method is required.");
        }
        boolean hasMci = tools.stream().anyMatch(t -> "MCI".equalsIgnoreCase(t.routingType()));
        String useCaseBaseName = toPascalCase(useCaseName);
        if (hasMci) {
            useCaseBaseName = abbreviatedMciSourceBaseName(useCaseBaseName);
        }
        String group = tools.getFirst().group().toLowerCase(Locale.ROOT);
        validateToolMethods(tools, group);

        String sourceDir = System.getProperty("AXHUB_SOURCE_DIR");
        if (sourceDir == null) {
            sourceDir = System.getenv("AXHUB_SOURCE_DIR");
        }
        Path rootDir = sourceDir != null ? Paths.get(sourceDir) : Paths.get(".");
        Path configuredModule = Paths.get(moduleName);
        Path moduleRoot = configuredModule.isAbsolute() ? configuredModule : rootDir.resolve(configuredModule);
        Path sourceRoot = moduleRoot.resolve(BASE_PACKAGE_PATH);
        Path useCaseDir = sourceRoot.resolve(Paths.get("biz", group, "usecase"));
        Path implDir = useCaseDir.resolve("impl");
        Path dtoDir = sourceRoot.resolve(Paths.get("biz", group, "dto"));
        Path converterDir = sourceRoot.resolve(Paths.get("biz", group, "converter"));
        Path definitionDir = moduleRoot.resolve(Paths.get("src", "main", "resources", "tool-definitions", group));
        Files.createDirectories(useCaseDir);
        Files.createDirectories(implDir);
        Files.createDirectories(dtoDir);
        Files.createDirectories(converterDir);
        Files.createDirectories(definitionDir);

        String bizPackage = BASE_PACKAGE + ".biz." + group;
        Path useCaseFile = useCaseDir.resolve(useCaseBaseName + "UseCase.java");
        Path useCaseImplFile = implDir.resolve(useCaseBaseName + "UseCaseImpl.java");
        Path converterFile = converterDir.resolve(useCaseBaseName + "Converter.java");
        boolean existingUseCase = Files.exists(useCaseFile);
        if (existingUseCase) {
            appendGroupedUseCaseSources(useCaseFile, useCaseImplFile, converterFile, bizPackage, useCaseBaseName,
                    moduleName, tools);
        } else {
            writeUtf8(useCaseFile, groupedUseCaseContent(bizPackage, useCaseBaseName, moduleName, tools));
            writeUtf8(useCaseImplFile, groupedUseCaseImplContent(bizPackage, useCaseBaseName, tools));
            if (!hasMci) {
                writeUtf8(converterFile, groupedConverterContent(bizPackage, useCaseBaseName, tools));
            }
        }

        StringBuilder log = new StringBuilder("\n=========================================\n")
                .append(" Multi Tool Scaffolding Complete\n")
                .append("=========================================\n")
                .append(existingUseCase ? " Existing UseCase Extended\n" : " New UseCase Created\n")
                .append("[Usecase Interface] ").append(useCaseFile).append("\n")
                .append("[Usecase Impl] ").append(useCaseImplFile).append("\n");
        for (ToolMethodDefinition tool : tools) {
            writeGroupedToolFiles(moduleRoot, sourceRoot, dtoDir, definitionDir, bizPackage, tool, moduleName, log);
            if ("HTTP".equalsIgnoreCase(tool.routingType())) {
                Path glowConfig = ensureLocalHttpApiConfiguration(moduleRoot, tool.httpApiName(),
                        toToolName(moduleName, tool.group(), toPascalCase(tool.baseName())));
                log.append("[HTTP Config] ").append(glowConfig).append("\n");
            }
        }
        if (!hasMci) {
            log.append("[Converter] ").append(converterFile).append("\n");
        }
        return log.toString();
    }

    private static void validateToolMethods(List<ToolMethodDefinition> tools, String expectedGroup) {
        Set<String> methods = new LinkedHashSet<>();
        Set<String> toolNames = new LinkedHashSet<>();
        for (ToolMethodDefinition tool : tools) {
            if (tool == null || tool.baseName() == null || tool.baseName().isBlank()
                    || tool.methodName() == null || !tool.methodName().matches("^[a-zA-Z_$][a-zA-Z0-9_$]*$")) {
                throw new IllegalArgumentException("Every Tool needs a valid base name and Java method name.");
            }
            if (!expectedGroup.equalsIgnoreCase(tool.group())) {
                throw new IllegalArgumentException("All Tool methods in one UseCase must use the same category.");
            }
            boolean mci = "MCI".equalsIgnoreCase(tool.routingType());
            boolean http = "HTTP".equalsIgnoreCase(tool.routingType());
            if (!mci && !http) {
                throw new IllegalArgumentException("Grouped Tool supports only MCI or HTTP routing.");
            }
            if (mci && (tool.interfaceId() == null || tool.interfaceId().isBlank()
                    || tool.clientSystemCode() == null || tool.clientSystemCode().isBlank())) {
                throw new IllegalArgumentException("MCI Tool needs an interface ID and Client system code.");
            }
            if (http && (tool.httpApiName() == null || tool.httpApiName().isBlank())) {
                throw new IllegalArgumentException("HTTP Tool needs an HTTP API name.");
            }
            String toolName = toToolName("", tool.group(), toPascalCase(tool.baseName()));
            if (!methods.add(tool.methodName()) || !toolNames.add(toolName)) {
                throw new IllegalArgumentException("Tool method names and MCP Tool names must be unique.");
            }
        }
    }

    private static String formatClientSystemCode(String code, String delimiter) {
        if (code == null) return "";
        if (code.length() == 9) {
            return code.substring(1, 4).toLowerCase(Locale.ROOT) + delimiter + code.substring(4, 5).toLowerCase(Locale.ROOT);
        } else if (code.length() == 4) {
            return code.substring(0, 3).toLowerCase(Locale.ROOT) + delimiter + code.substring(3).toLowerCase(Locale.ROOT);
        }
        return code.toLowerCase(Locale.ROOT);
    }

    private static String mciClientPrefix(String clientSystemCode) {
        String normalized = clientSystemCode == null ? "" : clientSystemCode.trim().toLowerCase(Locale.ROOT);
        return normalized.length() == 9 ? normalized.substring(1, 5) : normalized;
    }

    private static String mciClientClassName(String clientSystemCode) {
        return "Mci" + toPascalCase(mciClientPrefix(clientSystemCode)) + "Client";
    }

    private static String mciClientVariable(String clientSystemCode) {
        String prefix = toPascalCase(mciClientPrefix(clientSystemCode));
        return "mci" + prefix + "Client";
    }

    private static void writeGroupedToolFiles(Path moduleRoot, Path sourceRoot, Path dtoDir, Path definitionDir,
                                              String bizPackage, ToolMethodDefinition tool,
                                              String moduleName, StringBuilder log) throws IOException {
        boolean mci = "MCI".equalsIgnoreCase(tool.routingType());
        String toolBaseName = toPascalCase(tool.baseName());
        String baseName = mci ? abbreviatedMciSourceBaseName(toolBaseName) : toolBaseName;
        String code = mci ? formatClientSystemCode(tool.clientSystemCode(), "/") : toPackageSegment(tool.httpApiName());
        String ioPackage = BASE_PACKAGE + (mci ? ".infra.itrf.mci." : ".infra.itrf.http.") + code.replace("/", ".");
        Path clientDir = sourceRoot.resolve(Paths.get("infra", "itrf", mci ? "mci" : "http", code));
        Path ioDir = clientDir.resolve("io");
        Files.createDirectories(ioDir);
        writeUtf8(dtoDir.resolve(baseName + "Request.java"),
                dtoContent(bizPackage + ".dto", baseName + "Request", tool.inputFields(), "", "", true));
        writeUtf8(dtoDir.resolve(baseName + "Response.java"),
                dtoContent(bizPackage + ".dto", baseName + "Response", tool.outputFields(), "", "", false));
        writeStructuredFieldTypes(dtoDir, bizPackage + ".dto", baseName + "Request", tool.inputFields());
        writeStructuredFieldTypes(dtoDir, bizPackage + ".dto", baseName + "Response", tool.outputFields());
        if (mci) {
            String ioPrefix = (tool.clientSystemCode() != null && !tool.clientSystemCode().isBlank()) ? tool.clientSystemCode().toUpperCase() : tool.interfaceId();
            writeUtf8(ioDir.resolve(ioPrefix + "_I.java"),
                    mciIoContent("infra.itrf.mci." + code.replace("/", "."), ioPrefix + "_I", tool.inputFields(), "", ""));
            writeUtf8(ioDir.resolve(ioPrefix + "_O.java"),
                    mciIoContent("infra.itrf.mci." + code.replace("/", "."), ioPrefix + "_O", tool.outputFields(), "", ""));
            writeStructuredFieldTypes(ioDir, ioPackage + ".io", ioPrefix + "_I", tool.inputFields());
            writeStructuredFieldTypes(ioDir, ioPackage + ".io", ioPrefix + "_O", tool.outputFields());
            String sysCode = tool.clientSystemCode();
            if (sysCode != null && (sysCode.length() == 4 || sysCode.length() == 9)) {
                String clientCap = toPascalCase(mciClientPrefix(sysCode));
                String recSvcId = sysCode.toUpperCase();
                String itrfId = tool.interfaceId();
                writeUtf8(clientDir.resolve("Mci" + clientCap + "Client.java"),
                    "package " + ioPackage + ";\n\n"
                    + "import io.shinhanlife.dat.lib.integration.mci.component.AxhubMciComponent;\n"
                    + "import io.shinhanlife.glow.communication.dto.Transfer;\n"
                    + "import " + ioPackage + ".io." + ioPrefix + "_I;\n"
                    + "import " + ioPackage + ".io." + ioPrefix + "_O;\n"
                    + "import lombok.RequiredArgsConstructor;\n"
                    + "import org.springframework.stereotype.Component;\n\n"
                    + "@Component\n"
                    + "@RequiredArgsConstructor\n"
                    + "public class Mci" + clientCap + "Client {\n\n"
                    + "    private static final String INTERFACE_ID = \"" + itrfId + "\";\n"
                    + "    private static final String RECEIVE_SERVICE_ID = \"" + recSvcId + "\";\n\n"
                    + "    private final AxhubMciComponent mciComponent;\n\n"
                    + "    public <O> Transfer<O> callTo(String interfaceId, String receiveServiceId, Object mciReq, Class<O> resType) {\n"
                    + "        return mciComponent.callTo(interfaceId, receiveServiceId, mciReq, resType);\n"
                    + "    }\n\n"
                    + "    public " + ioPrefix + "_O call" + toPascalCase(ioPrefix) + "(" + ioPrefix + "_I request) {\n"
                    + "        Transfer<" + ioPrefix + "_O> transfer = mciComponent.callTo(\n"
                    + "                INTERFACE_ID, RECEIVE_SERVICE_ID, request, " + ioPrefix + "_O.class);\n"
                    + "        return transfer == null ? null : transfer.getBody();\n"
                    + "    }\n"
                    + "}\n");
            } else {
                writeUtf8(clientDir.resolve(baseName + "Client.java"), groupedMciClientContent(ioPackage, baseName, tool.interfaceId()));
            }
            String targetPkg = mciTargetSystemPackage(tool.clientSystemCode());
            String converterPkg = bizPackage + ".converter" + (targetPkg != null ? "." + targetPkg : "");
            Path targetConverterDir = sourceRoot.resolve(Paths.get("biz", tool.group().toLowerCase(Locale.ROOT), "converter"));
            if (targetPkg != null) {
                for (String seg : targetPkg.split("\\.")) {
                    targetConverterDir = targetConverterDir.resolve(seg);
                }
            }
            Files.createDirectories(targetConverterDir);
            String converterName = (tool.clientSystemCode() != null && !tool.clientSystemCode().isBlank())
                    ? tool.clientSystemCode().toUpperCase(Locale.ROOT) + "Converter"
                    : baseName + "Converter";
            writeUtf8(targetConverterDir.resolve(converterName + ".java"),
                    groupedMciConverterContent(converterPkg, bizPackage, baseName, ioPackage, ioPrefix, converterName));
        } else {
            writeUtf8(ioDir.resolve(baseName + "HttpRequest.java"),
                    dtoContent(ioPackage + ".io", baseName + "HttpRequest", tool.inputFields(), "", "", true));
            writeUtf8(ioDir.resolve(baseName + "HttpResponse.java"),
                    dtoContent(ioPackage + ".io", baseName + "HttpResponse", tool.outputFields(), "", "", false));
            writeStructuredFieldTypes(ioDir, ioPackage + ".io", baseName + "HttpRequest", tool.inputFields());
            writeStructuredFieldTypes(ioDir, ioPackage + ".io", baseName + "HttpResponse", tool.outputFields());
            writeUtf8(clientDir.resolve(baseName + "Client.java"),
                    httpClientContent(ioPackage, baseName + "Client", tool.httpApiName()));
            writeUtf8(sourceRoot.resolve(Paths.get("biz", tool.group().toLowerCase(Locale.ROOT), "converter", baseName + "Converter.java")),
                    groupedHttpConverterContent(bizPackage, baseName, ioPackage));
        }

        String toolName = toToolName(moduleName, tool.group(), toolBaseName);
        log.append("[Tool] ").append(toolName).append(" -> ").append(clientDir.resolve(baseName + "Client.java")).append("\n");
    }

    private static String groupedUseCaseContent(String bizPackage, String useCaseBaseName, String moduleName,
                                                List<ToolMethodDefinition> tools) {
        StringBuilder imports = new StringBuilder();
        StringBuilder methods = new StringBuilder();
        for (ToolMethodDefinition tool : tools) {
            boolean mci = "MCI".equalsIgnoreCase(tool.routingType());
            String toolBaseName = toPascalCase(tool.baseName());
            String baseName = mci ? abbreviatedMciSourceBaseName(toolBaseName) : toolBaseName;
            imports.append("import ").append(bizPackage).append(".dto.").append(baseName).append("Request;\n")
                    .append("import ").append(bizPackage).append(".dto.").append(baseName).append("Response;\n");
            boolean isMutation = isMutationTool(baseName);
            ToolDefinitionOptions opts = tool.definitionOptions() == null ? new ToolDefinitionOptions(null, null, null, null, null, null, null, null) : tool.definitionOptions();
            
            methods.append("    @McpTool(name = \"").append(toToolName(moduleName, tool.group(), toolBaseName))
                    .append("\", title = \"").append(javaText(option(tool.title(), baseName)))
                    .append("\", description = \"").append(javaText(option(tool.description(), ""))).append("\")\n")
                    .append("    @GrowToolHint(\n")
                    .append("        requiresApproval = ").append(isMutation).append(",\n")
                    .append("        categoryKey = \"").append(tool.group().toLowerCase(Locale.ROOT)).append("\",\n")
                    .append("        timeoutMillis = ").append(opts.timeoutMillisOrDefault()).append("L,\n")
                    .append("        retryMaxAttempts = ").append(opts.retryMaxAttemptsOrDefault()).append(",\n");
            if (tool.interfaceId() != null && !tool.interfaceId().isBlank()) {
                methods.append("        mappingId = \"").append(javaText(tool.interfaceId())).append("\",\n");
            }
            methods.append("        functionDescription = \"").append(javaText(opts.functionDescription() != null && !opts.functionDescription().isBlank() ? opts.functionDescription() : option(tool.title(), baseName) + " 기능을 수행합니다.")).append("\",\n")
                    .append("        whenToUse = \"").append(javaText(opts.whenToUse() != null && !opts.whenToUse().isBlank() ? opts.whenToUse() : "사용자가 이 업무 기능의 실행 또는 조회를 요청할 때 사용합니다.")).append("\",\n")
                    .append("        whenNotToUse = \"").append(javaText(opts.whenNotToUse() != null && !opts.whenNotToUse().isBlank() ? opts.whenNotToUse() : "정보 변경이나 실행 작업에는 사용하지 않습니다.")).append("\",\n")
                    .append("        ioLimits = \"").append(javaText(opts.ioLimits() != null && !opts.ioLimits().isBlank() ? opts.ioLimits() : "정의된 입력 항목만 허용하며 업무 결과만 반환합니다.")).append("\",\n")
                    .append("        displayDescription = \"").append(javaText(opts.displayDescription() != null && !opts.displayDescription().isBlank() ? opts.displayDescription() : option(tool.title(), baseName) + " 정보를 처리합니다.")).append("\",\n");
                    
            List<String> examples = opts.exampleQueries();
            if (examples == null || examples.isEmpty()) {
                examples = List.of(option(tool.title(), baseName) + " 정보를 보여줘", option(tool.title(), baseName) + " 확인해줘", "현재 " + option(tool.title(), baseName) + " 알려줘");
            }
            methods.append("        exampleQueries = {")
                    .append(examples.stream().map(q -> "\"" + javaText(q) + "\"").collect(Collectors.joining(", ")))
                    .append("},\n")
                    .append("        destructive = ").append(isMutation).append(",\n")
                    .append("        idempotent = ").append(!isMutation).append(",\n");
                    
            List<String> tags = opts.tags();
            if (tags == null || tags.isEmpty()) {
                tags = List.of(tool.group().toLowerCase(Locale.ROOT), isMutation ? "처리" : "조회");
            }
            methods.append("        tags = {")
                    .append(tags.stream().map(t -> "\"" + javaText(t) + "\"").collect(Collectors.joining(", ")))
                    .append("},\n")
                    .append("        ownerOrg = \"").append(javaText(opts.ownerOrg() != null && !opts.ownerOrg().isBlank() ? opts.ownerOrg() : "MCP_TOOL")).append("\"\n")
                    .append("    )\n")
                    .append("    ").append(baseName).append("Response ").append(tool.methodName()).append("(")
                    .append(baseName).append("Request req);\n\n");
        }
        return "package " + bizPackage + ".usecase;\n\n"
                + "import org.springaicommunity.mcp.annotation.McpTool;\n"
                + "import io.shinhanlife.dat.lib.annotation.GrowToolHint;\n"
                + imports + "\npublic interface " + useCaseBaseName + "UseCase {\n\n" + methods + "}\n";
    }

    private static String groupedUseCaseImplContent(String bizPackage, String useCaseBaseName,
                                                    List<ToolMethodDefinition> tools) {
        StringBuilder imports = new StringBuilder();
        StringBuilder fields = new StringBuilder();
        StringBuilder methods = new StringBuilder();
        for (ToolMethodDefinition tool : tools) {
            boolean mci = "MCI".equalsIgnoreCase(tool.routingType());
            String toolBaseName = toPascalCase(tool.baseName());
            String baseName = mci ? abbreviatedMciSourceBaseName(toolBaseName) : toolBaseName;
            String clientClassName;
            String clientVariable;
            String ioPrefix = (tool.clientSystemCode() != null && !tool.clientSystemCode().isBlank()) ? tool.clientSystemCode().toUpperCase() : tool.interfaceId();
            if (mci) {
                clientClassName = mciClientClassName(tool.clientSystemCode());
                clientVariable = "mci";
            } else {
                clientClassName = baseName + "Client";
                clientVariable = Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1) + "Client";
            }
            String targetPkg = mci ? mciTargetSystemPackage(tool.clientSystemCode()) : null;
            String converterPkg = bizPackage + ".converter" + (targetPkg != null ? "." + targetPkg : "");
            String converterName = (mci && tool.clientSystemCode() != null && !tool.clientSystemCode().isBlank())
                    ? tool.clientSystemCode().toUpperCase(Locale.ROOT) + "Converter"
                    : baseName + "Converter";
            String converterVariable = "converter";
            String integrationPackage = BASE_PACKAGE + (mci ? ".infra.itrf.mci." + formatClientSystemCode(tool.clientSystemCode(), ".")
                    : ".infra.itrf.http." + toPackageSegment(tool.httpApiName()));
            imports.append("import ").append(bizPackage).append(".dto.").append(baseName).append("Request;\n")
                    .append("import ").append(bizPackage).append(".dto.").append(baseName).append("Response;\n")
                    .append("import ").append(converterPkg).append(".").append(converterName).append(";\n")
                    .append("import ").append(integrationPackage).append(".").append(clientClassName).append(";\n")
                    .append("import ").append(integrationPackage).append(".io.").append(mci ? ioPrefix + "_I;\n" : baseName + "HttpRequest;\n")
                    .append("import ").append(integrationPackage).append(".io.").append(mci ? ioPrefix + "_O;\n" : baseName + "HttpResponse;\n");
            if (mci) {
                imports.append("import io.shinhanlife.glow.communication.dto.Transfer;\n");
                imports.append("import lombok.extern.slf4j.Slf4j;\n");
            }
            if (!fields.toString().contains(" " + clientVariable + ";")) {
                fields.append("    private final ").append(clientClassName).append(" ").append(clientVariable).append(";\n");
            }
            if (!fields.toString().contains(" " + converterVariable + ";")) {
                fields.append("    private final ").append(converterName).append(" ").append(converterVariable).append(";\n");
            }
            methods.append(groupedToolMethodContent(tool, baseName, converterVariable, clientVariable, ioPrefix, mci));
        }
        String slf4jAnno = tools.stream().anyMatch(t -> "MCI".equalsIgnoreCase(t.routingType())) ? "@Slf4j\n" : "";
        return "package " + bizPackage + ".usecase.impl;\n\n"
                + "import " + bizPackage + ".usecase." + useCaseBaseName + "UseCase;\n"
                + "import lombok.RequiredArgsConstructor;\nimport org.springframework.stereotype.Service;\n" + imports
                + "\n" + slf4jAnno + "@Service\n@RequiredArgsConstructor\npublic class " + useCaseBaseName + "UseCaseImpl implements " + useCaseBaseName + "UseCase {\n\n"
                + fields + "\n" + methods + "}\n";
    }

    private static String groupedToolMethodContent(ToolMethodDefinition tool, String baseName,
                                                   String converterVariable, String clientVariable,
                                                   String ioPrefix, boolean mci) {
        if (mci) {
            String snakeToolName = (tool.group() != null && !tool.group().isBlank() ? tool.group().toLowerCase(Locale.ROOT) + "_" : "")
                    + toKebabCase(baseName).toLowerCase(Locale.ROOT).replace("-", "_");
            String receiveServiceId = tool.clientSystemCode() != null ? tool.clientSystemCode().toUpperCase() : "";
            return """

                    @Override
                    public %sResponse %s(%sRequest req) {
                        log.info("[MCI Tool] {} 요청 수신.", "%s");
                        try {
                            // MapStruct를 이용한 자동 매핑 (AI DTO -> MCI DTO)
                            %s_I mciReq = %s.toRequest(req);

                            Transfer<%s_O> resTransfer = %s.callTo(
                                    "%s",
                                    "%s",
                                    mciReq,
                                    %s_O.class
                            );
                            %sResponse response = new %sResponse();
                            if (resTransfer != null && resTransfer.getBody() != null) {
                                response = %s.toResponse(resTransfer.getBody());
                            }
                            response.setResultCode("SUCCESS");
                            return response;
                        } catch (Exception e) {
                            log.error("[MCI Tool] 연동 중 오류 발생: {}", e.getMessage(), e);
                            %sResponse errorResponse = new %sResponse();
                            errorResponse.setResultCode("ERROR");
                            errorResponse.setResultMessage("MCI call failed: " + e.getMessage());
                            return errorResponse;
                        }
                    }
                    """.formatted(baseName, tool.methodName(), baseName, snakeToolName,
                    ioPrefix, converterVariable,
                    ioPrefix, clientVariable, tool.interfaceId(), receiveServiceId, ioPrefix,
                    baseName, baseName,
                    converterVariable,
                    baseName, baseName);
        }
        return """

                @Override
                public %sResponse %s(%sRequest req) {
                    %sHttpRequest request = %s.toRequest(req);
                    %sHttpResponse response = %s.call(request, %sHttpResponse.class);
                    %sResponse toolResponse = %s.toResponse(response);
                    if (toolResponse == null) toolResponse = new %sResponse();
                    toolResponse.setResultCode("SUCCESS");
                    return toolResponse;
                }
                """.formatted(baseName, tool.methodName(), baseName, baseName, converterVariable, baseName,
                clientVariable, baseName, baseName, converterVariable, baseName);
    }

    private static String groupedConverterContent(String bizPackage, String useCaseBaseName,
                                                  List<ToolMethodDefinition> tools) {
        return "package " + bizPackage + ".converter;\n\n/** Per-Tool converters are generated beside this compatibility marker. */\n"
                + "public interface " + useCaseBaseName + "Converter {\n}\n";
    }

    private static String groupedMciConverterContent(String converterPackage, String bizPackage, String baseName, String ioPackage, String interfaceId, String converterName) {
        return "package " + converterPackage + ";\n\n"
                + "import " + bizPackage + ".dto." + baseName + "Request;\n"
                + "import " + bizPackage + ".dto." + baseName + "Response;\n"
                + "import " + ioPackage + ".io." + interfaceId + "_I;\n"
                + "import " + ioPackage + ".io." + interfaceId + "_O;\n"
                + "import org.mapstruct.Mapper;\nimport org.mapstruct.ReportingPolicy;\n\n"
                + "@Mapper(componentModel = \"spring\", unmappedTargetPolicy = ReportingPolicy.IGNORE)\n"
                + "public interface " + converterName + " {\n"
                + "    " + interfaceId + "_I toRequest(" + baseName + "Request request);\n"
                + "    " + baseName + "Response toResponse(" + interfaceId + "_O response);\n}\n";
    }

    private static String groupedHttpConverterContent(String bizPackage, String baseName, String ioPackage) {
        return "package " + bizPackage + ".converter;\n\n"
                + "import " + bizPackage + ".dto." + baseName + "Request;\n"
                + "import " + bizPackage + ".dto." + baseName + "Response;\n"
                + "import " + ioPackage + ".io." + baseName + "HttpRequest;\n"
                + "import " + ioPackage + ".io." + baseName + "HttpResponse;\n"
                + "import org.mapstruct.Mapper;\nimport org.mapstruct.ReportingPolicy;\n\n"
                + "@Mapper(componentModel = \"spring\", unmappedTargetPolicy = ReportingPolicy.IGNORE)\n"
                + "public interface " + baseName + "Converter {\n"
                + "    " + baseName + "HttpRequest toRequest(" + baseName + "Request request);\n"
                + "    " + baseName + "Response toResponse(" + baseName + "HttpResponse response);\n}\n";
    }

    private static void appendGroupedUseCaseSources(Path useCaseFile, Path useCaseImplFile, Path converterFile,
                                                    String bizPackage, String useCaseBaseName, String moduleName,
                                                    List<ToolMethodDefinition> tools) throws IOException {
        if (!Files.exists(useCaseImplFile)) {
            throw new IllegalArgumentException("UseCase implementation not found: " + useCaseImplFile);
        }
        String useCase = Files.readString(useCaseFile, StandardCharsets.UTF_8);
        String implementation = Files.readString(useCaseImplFile, StandardCharsets.UTF_8);
        for (ToolMethodDefinition tool : tools) {
            boolean mci = "MCI".equalsIgnoreCase(tool.routingType());
            String toolBaseName = toPascalCase(tool.baseName());
            String baseName = mci ? abbreviatedMciSourceBaseName(toolBaseName) : toolBaseName;
            String methodName = tool.methodName();
            String toolName = toToolName(moduleName, tool.group(), toolBaseName);
            if (useCase.matches("(?s).*\\b" + java.util.regex.Pattern.quote(methodName) + "\\s*\\(.*")
                    || useCase.contains("name = \"" + toolName + "\"")) {
                throw new IllegalArgumentException("Tool method or MCP Tool name already exists: " + methodName);
            }
            String integrationPackage = BASE_PACKAGE + (mci ? ".infra.itrf.mci." + formatClientSystemCode(tool.clientSystemCode(), ".")
                    : ".infra.itrf.http." + toPackageSegment(tool.httpApiName()));
            String requestType = baseName + "Request";
            String responseType = baseName + "Response";
            String ioPrefix = (tool.clientSystemCode() != null && !tool.clientSystemCode().isBlank()) ? tool.clientSystemCode().toUpperCase() : tool.interfaceId();
            String requestIo = mci ? ioPrefix + "_I" : baseName + "HttpRequest";
            String responseIo = mci ? ioPrefix + "_O" : baseName + "HttpResponse";

            useCase = addImport(useCase, "import " + bizPackage + ".dto." + requestType + ";") ;
            useCase = addImport(useCase, "import " + bizPackage + ".dto." + responseType + ";") ;
            boolean isMutation = isMutationTool(baseName);
            ToolDefinitionOptions opts = tool.definitionOptions() == null ? new ToolDefinitionOptions(null, null, null, null, null, null, null, null) : tool.definitionOptions();
            
            StringBuilder declBuilder = new StringBuilder("\n    @McpTool(name = \"").append(toolName)
                    .append("\", title = \"").append(javaText(option(tool.title(), baseName)))
                    .append("\", description = \"").append(javaText(option(tool.description(), ""))).append("\")\n")
                    .append("    @GrowToolHint(\n")
                    .append("        requiresApproval = ").append(isMutation).append(",\n")
                    .append("        categoryKey = \"").append(tool.group().toLowerCase(Locale.ROOT)).append("\",\n")
                    .append("        timeoutMillis = ").append(opts.timeoutMillisOrDefault()).append("L,\n")
                    .append("        retryMaxAttempts = ").append(opts.retryMaxAttemptsOrDefault()).append(",\n");
            String mappingId = option(tool.interfaceId(), tool.httpApiName());
            if (mappingId != null && !mappingId.isBlank()) {
                declBuilder.append("        mappingId = \"").append(javaText(mappingId)).append("\",\n");
            }
            declBuilder.append("        functionDescription = \"").append(javaText(opts.functionDescription() != null && !opts.functionDescription().isBlank() ? opts.functionDescription() : option(tool.title(), baseName) + " 기능을 수행합니다.")).append("\",\n")
                    .append("        whenToUse = \"").append(javaText(opts.whenToUse() != null && !opts.whenToUse().isBlank() ? opts.whenToUse() : "사용자가 이 업무 기능의 실행 또는 조회를 요청할 때 사용합니다.")).append("\",\n")
                    .append("        whenNotToUse = \"").append(javaText(opts.whenNotToUse() != null && !opts.whenNotToUse().isBlank() ? opts.whenNotToUse() : "정보 변경이나 실행 작업에는 사용하지 않습니다.")).append("\",\n")
                    .append("        ioLimits = \"").append(javaText(opts.ioLimits() != null && !opts.ioLimits().isBlank() ? opts.ioLimits() : "정의된 입력 항목만 허용하며 업무 결과만 반환합니다.")).append("\",\n")
                    .append("        displayDescription = \"").append(javaText(opts.displayDescription() != null && !opts.displayDescription().isBlank() ? opts.displayDescription() : option(tool.title(), baseName) + " 정보를 처리합니다.")).append("\",\n");
                    
            List<String> examples = opts.exampleQueries();
            if (examples == null || examples.isEmpty()) {
                examples = List.of(option(tool.title(), baseName) + " 정보를 보여줘", option(tool.title(), baseName) + " 확인해줘", "현재 " + option(tool.title(), baseName) + " 알려줘");
            }
            declBuilder.append("        exampleQueries = {")
                    .append(examples.stream().map(q -> "\"" + javaText(q) + "\"").collect(Collectors.joining(", ")))
                    .append("},\n")
                    .append("        destructive = ").append(isMutation).append(",\n")
                    .append("        idempotent = ").append(!isMutation).append(",\n");
                    
            List<String> tags = opts.tags();
            if (tags == null || tags.isEmpty()) {
                tags = List.of(tool.group().toLowerCase(Locale.ROOT), isMutation ? "처리" : "조회");
            }
            declBuilder.append("        tags = {")
                    .append(tags.stream().map(t -> "\"" + javaText(t) + "\"").collect(Collectors.joining(", ")))
                    .append("},\n")
                    .append("        ownerOrg = \"").append(javaText(opts.ownerOrg() != null && !opts.ownerOrg().isBlank() ? opts.ownerOrg() : "MCP_TOOL")).append("\"\n")
                    .append("    )\n")
                    .append("    ").append(responseType).append(" ").append(methodName).append("(").append(requestType).append(" req);\n");
            
            String declaration = declBuilder.toString();
            useCase = insertBeforeLastBrace(useCase, declaration);

            String clientClassName;
            String clientVariable;
            if (mci) {
                clientClassName = mciClientClassName(tool.clientSystemCode());
                clientVariable = "mci";
            } else {
                clientClassName = baseName + "Client";
                clientVariable = Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1) + "Client";
            }
            String targetPkg = mci ? mciTargetSystemPackage(tool.clientSystemCode()) : null;
            String converterPkg = bizPackage + ".converter" + (targetPkg != null ? "." + targetPkg : "");
            String converterName = (mci && tool.clientSystemCode() != null && !tool.clientSystemCode().isBlank())
                    ? tool.clientSystemCode().toUpperCase(Locale.ROOT) + "Converter"
                    : baseName + "Converter";
            String converterVariable = "converter";

            implementation = addImport(implementation, "import " + bizPackage + ".dto." + requestType + ";");
            implementation = addImport(implementation, "import " + bizPackage + ".dto." + responseType + ";");
            implementation = addImport(implementation, "import " + converterPkg + "." + converterName + ";");
            implementation = addImport(implementation, "import " + integrationPackage + "." + clientClassName + ";");
            implementation = addImport(implementation, "import " + integrationPackage + ".io." + requestIo + ";");
            implementation = addImport(implementation, "import " + integrationPackage + ".io." + responseIo + ";");
            if (mci) {
                implementation = addImport(implementation, "import io.shinhanlife.glow.communication.dto.Transfer;");
                implementation = addImport(implementation, "import lombok.extern.slf4j.Slf4j;");
                if (!implementation.contains("@Slf4j")) {
                    implementation = implementation.replaceFirst("public class ", "@Slf4j\npublic class ");
                }
            }
            implementation = addImport(implementation, "import lombok.RequiredArgsConstructor;");
            if (!implementation.contains("@RequiredArgsConstructor")) {
                implementation = implementation.replaceFirst("public class ", "@RequiredArgsConstructor\npublic class ");
            }
            if (!implementation.contains(" " + clientVariable + ";")) {
                implementation = insertConstructorField(implementation, "    private final " + clientClassName + " " + clientVariable + ";");
            }
            if (!implementation.contains(" " + converterVariable + ";")) {
                implementation = insertConstructorField(implementation, "    private final " + converterName + " " + converterVariable + ";");
            }
            String method = groupedToolMethodContent(tool, baseName, converterVariable, clientVariable, ioPrefix, mci);
            implementation = insertBeforeLastBrace(implementation, method);
        }
        writeUtf8(useCaseFile, useCase);
        writeUtf8(useCaseImplFile, implementation);
        boolean hasMci = tools.stream().anyMatch(t -> "MCI".equalsIgnoreCase(t.routingType()));
        if (!hasMci && !Files.exists(converterFile)) {
            writeUtf8(converterFile, groupedConverterContent(bizPackage, useCaseBaseName, tools));
        }
    }

    private static String addImport(String content, String importLine) {
        if (content.contains(importLine)) return content;
        int lastImport = content.lastIndexOf("import ");
        if (lastImport < 0) {
            int packageEnd = content.indexOf(';');
            return content.substring(0, packageEnd + 1) + "\n\n" + importLine + content.substring(packageEnd + 1);
        }
        int lineEnd = content.indexOf('\n', lastImport);
        return content.substring(0, lineEnd + 1) + importLine + "\n" + content.substring(lineEnd + 1);
    }

    private static String insertConstructorField(String content, String field) {
        if (content.contains(field)) return content;
        int constructorField = content.lastIndexOf("private final ");
        if (constructorField < 0) {
            int classDecl = content.indexOf("public class ");
            if (classDecl >= 0) {
                int brace = content.indexOf('{', classDecl);
                if (brace >= 0) return content.substring(0, brace + 1) + "\n" + field + content.substring(brace + 1);
            }
            return insertBeforeLastBrace(content, "\n" + field + "\n");
        }
        int lineEnd = content.indexOf('\n', constructorField);
        if (lineEnd < 0) return content + "\n" + field;
        return content.substring(0, lineEnd + 1) + field + "\n" + content.substring(lineEnd + 1);
    }

    private static String insertBeforeLastBrace(String content, String addition) {
        int brace = content.lastIndexOf('}');
        if (brace < 0) throw new IllegalArgumentException("Java source closing brace not found.");
        return content.substring(0, brace) + addition + content.substring(brace);
    }

    private static String groupedMciClientContent(String ioPackage, String baseName, String interfaceId) {
        return "package " + ioPackage + ";\n\n"
                + "import io.shinhanlife.dat.lib.integration.mci.component.AxhubMciComponent;\n"
                + "import io.shinhanlife.glow.communication.dto.Transfer;\n"
                + "import lombok.RequiredArgsConstructor;\nimport org.springframework.stereotype.Component;\n"
                + "import " + ioPackage + ".io." + baseName + "_I;\n"
                + "import " + ioPackage + ".io." + baseName + "_O;\n\n"
                + "@Component\n@RequiredArgsConstructor\npublic class " + baseName + "Client {\n"
                + "    private final AxhubMciComponent mci;\n\n"
                + "    public " + baseName + "_O call" + baseName + "(" + baseName + "_I request) {\n"
                + "        try {\n"
                + "            Transfer<" + baseName + "_O> transfer = mci.callTo(\"" + javaText(interfaceId) + "\", null, request, " + baseName + "_O.class);\n"
                + "            return transfer.getBody();\n"
                + "        } catch (Exception e) {\n"
                + "            throw new IllegalStateException(\"MCI call failed: " + javaText(interfaceId) + "\", e);\n"
                + "        }\n    }\n}\n";
    }

    private static String javaText(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=========================================");
        System.out.println("   MCP Tool Scaffolder (Java CLI)   ");
        System.out.println("=========================================\n");

        String baseName = getOrAsk(args, 0, scanner, "1. 생성할 Tool의 기본 이름 (예: ExchangeRate) [영문 PascalCase]: ");
        String interfaceId = getOrAsk(args, 1, scanner, "2. 레거시 API 인터페이스 ID (예: EXCH_001): ");
        String title = getOrAsk(args, 2, scanner, "3. Tool title: ");
        String description = getOrAsk(args, 3, scanner, "4. Tool description for LLM: ");
        String group = getOrAsk(args, 4, scanner, "5. Tool category: ");
        if (group.isEmpty()) group = "COMMON";
        String routingType = getOrAsk(args, 5, scanner, "6. Routing type (HTTP, TCP, MCI, EAI): ");
        if (routingType.trim().isEmpty()) {
            routingType = "MCI";
        }
        String moduleName = getOrAsk(args, 6, scanner, "7. Target module (default dat-was-cus): ");
        if (moduleName.trim().isEmpty()) {
            moduleName = "dat-was-cus";
        }

        String defaultAuthor = System.getProperty("user.name");
        String defaultDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

        String author = getOrAsk(args, 7, scanner, "8. 작성자(Enter 입력 시 '" + defaultAuthor + "'): ");
        if (author.trim().isEmpty()) author = defaultAuthor;
        String createDate = getOrAsk(args, 8, scanner, "9. 작성일(Enter 입력 시 '" + defaultDate + "'): ");
        if (createDate.trim().isEmpty()) createDate = defaultDate;

        String useSchemaResourceStr = getOrAsk(args, 9, scanner, "10. input/output JSON Schema 파일 자동 생성 여부 (y/N): ");
        boolean useSchemaResource = "y".equalsIgnoreCase(useSchemaResourceStr.trim());

        String schemaResourceDirectory = "classpath:tool-schemas/" + group.toLowerCase() + "/";
        String inputSchemaResource = useSchemaResource ? schemaResourceDirectory + toKebabCase(baseName) + "-resource-input-schema.json" : null;
        String outputSchemaResource = useSchemaResource ? schemaResourceDirectory + toKebabCase(baseName) + "-resource-output-schema.json" : null;
        String defaultWorkspace = "C:\\eGovFrameDev-4.3.1-64bit\\workspace-egov\\dat-was-dasmt";
        String workspace = getOrAsk(args, 10, scanner, "11. 대상 프로젝트 워크스페이스 경로 (default: " + defaultWorkspace + "): ");
        if (workspace.trim().isEmpty()) {
            workspace = defaultWorkspace;
        }
        System.setProperty("AXHUB_SOURCE_DIR", workspace);

        String result = scaffold(baseName, interfaceId, title, description, group, routingType, moduleName, author, createDate, false, null, inputSchemaResource, outputSchemaResource, List.of(new FieldDefinition("query", "String", "Search query", List.of("example"), "", false)), List.of());
        System.out.println(result);
    }

    private static String getOrAsk(String[] args, int index, Scanner scanner, String prompt) {
        if (args.length > index) {
            return args[index];
        }
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static String scaffold(String baseName, String interfaceId, String description, String group, String routingType, String moduleName, String author, String createDate, boolean register, String clientSystemCode) throws IOException {
        return scaffold(baseName, interfaceId, description, group, routingType, moduleName, author, createDate, register, clientSystemCode, null, null);
    }

    public static String scaffold(String baseName, String interfaceId, String description, String group, String routingType, String moduleName, String author, String createDate, boolean register, String clientSystemCode, String inputSchemaResource, String outputSchemaResource) throws IOException {
        return scaffold(baseName, interfaceId, description, group, routingType, moduleName, author, createDate,
                register, clientSystemCode, inputSchemaResource, outputSchemaResource,
                List.of(new FieldDefinition("query", "String", "Search query", List.of("example"), "", false)), List.of());
    }

    public static String scaffold(String baseName, String interfaceId, String description, String group, String routingType, String moduleName, String author, String createDate, boolean register, String clientSystemCode, String inputSchemaResource, String outputSchemaResource, List<FieldDefinition> inputFields, List<FieldDefinition> outputFields) throws IOException {
        return scaffold(baseName, interfaceId, description, description, group, routingType, moduleName, author, createDate,
                register, clientSystemCode, inputSchemaResource, outputSchemaResource, inputFields, outputFields);
    }

    /**
     * Generates a Tool with a human-facing title and an LLM-facing description.
     * Existing overloads keep their previous behavior by using the description as the title.
     */
    public static String scaffold(String baseName, String interfaceId, String title, String description, String group, String routingType, String moduleName, String author, String createDate, boolean register, String clientSystemCode, String inputSchemaResource, String outputSchemaResource, List<FieldDefinition> inputFields, List<FieldDefinition> outputFields) throws IOException {
        return scaffold(baseName, interfaceId, title, description, group, routingType, moduleName, author, createDate,
                register, clientSystemCode, inputSchemaResource, outputSchemaResource, inputFields, outputFields,
                toKebabCase(toPascalCase(baseName)));
    }

    /**
     * Generates a Tool using an HTTP API name that is resolved from glow.communication.http.api-list.
     */
    public static String scaffold(String baseName, String interfaceId, String title, String description, String group, String routingType, String moduleName, String author, String createDate, boolean register, String clientSystemCode, String inputSchemaResource, String outputSchemaResource, List<FieldDefinition> inputFields, List<FieldDefinition> outputFields, String httpApiName) throws IOException {
        return scaffold(baseName, interfaceId, title, description, group, routingType, moduleName, author, createDate,
                register, clientSystemCode, inputSchemaResource, outputSchemaResource, inputFields, outputFields,
                httpApiName, null);
    }

    public static String scaffold(String baseName, String interfaceId, String title, String description, String group,
                                  String routingType, String moduleName, String author, String createDate,
                                  boolean register, String clientSystemCode, String inputSchemaResource,
                                  String outputSchemaResource, List<FieldDefinition> inputFields,
                                  List<FieldDefinition> outputFields, String httpApiName,
                                  ToolDefinitionOptions definitionOptions) throws IOException {
        return scaffoldWithAbbreviatedMciSources(baseName, interfaceId, title, description, group, routingType,
                moduleName, author, createDate, register, clientSystemCode, inputSchemaResource,
                outputSchemaResource, inputFields, outputFields, httpApiName, definitionOptions);
    }

    /**
     * Generates MCI source files from an abbreviation derived from the Base Name. The Base Name remains the MCP Tool name.
     */
    private static String scaffoldWithAbbreviatedMciSources(String baseName, String interfaceId, String title,
                                                            String description, String group, String routingType,
                                                            String moduleName, String author, String createDate,
                                                            boolean register, String clientSystemCode,
                                                            String inputSchemaResource, String outputSchemaResource,
                                                            List<FieldDefinition> inputFields,
                                                            List<FieldDefinition> outputFields, String httpApiName,
                                                            ToolDefinitionOptions definitionOptions) throws IOException {
        boolean isMci = "MCI".equalsIgnoreCase(routingType);
        boolean isHttp = "HTTP".equalsIgnoreCase(routingType);
        if (!isMci && !isHttp) {
            throw new IllegalArgumentException("Unsupported routing type: " + routingType + ". Only MCI and HTTP are supported.");
        }
        String toolBaseName = toPascalCase(baseName);
        baseName = isMci ? abbreviatedMciSourceBaseName(toolBaseName) : toolBaseName;
        title = title == null || title.isBlank() ? toolBaseName : title.trim();
        description = description == null ? "" : description.trim();
        definitionOptions = definitionOptions == null
                ? new ToolDefinitionOptions(null, null, null, null, null, List.of(), List.of(), null)
                : definitionOptions;
        httpApiName = httpApiName == null || httpApiName.isBlank() ? toKebabCase(toolBaseName) : httpApiName.trim();
        String envSourceDir = System.getProperty("AXHUB_SOURCE_DIR");
        if (envSourceDir == null) {
            envSourceDir = System.getenv("AXHUB_SOURCE_DIR");
        }
        Path rootDir = envSourceDir != null ? Paths.get(envSourceDir) : Paths.get(".");

        Path usecaseDir = rootDir.resolve(Paths.get(moduleName, BASE_PACKAGE_PATH, "biz", group.toLowerCase(), "usecase"));
        Path usecaseImplDir = usecaseDir.resolve("impl");
        Path dtoDir = rootDir.resolve(Paths.get(moduleName, BASE_PACKAGE_PATH, "biz", group.toLowerCase(), "dto"));

        Path legacyDtoDir = rootDir.resolve(Paths.get(moduleName, BASE_PACKAGE_PATH, "biz", group.toLowerCase(), "legacy"));

        // Schema Resource 파일 경로(useSchemaResource=true일 때만 생성)
        boolean useSchemaResource = (inputSchemaResource != null && !inputSchemaResource.trim().isEmpty()) || (outputSchemaResource != null && !outputSchemaResource.trim().isEmpty());
        String schemaBaseName = toKebabCase(toolBaseName);
        String inputSchemaFileName  = schemaBaseName + "-resource-input-schema.json";
        String outputSchemaFileName = schemaBaseName + "-resource-output-schema.json";
        Path schemaDir = rootDir.resolve(Paths.get(moduleName, "src/main/resources/tool-schemas", group.toLowerCase()));
        Path definitionDir = rootDir.resolve(Paths.get(moduleName, "src/main/resources/tool-definitions", group.toLowerCase()));
        String inputSchemaClasspath  = "classpath:tool-schemas/" + group.toLowerCase() + "/" + inputSchemaFileName;
        String outputSchemaClasspath = "classpath:tool-schemas/" + group.toLowerCase() + "/" + outputSchemaFileName;

        String bizPackage = BASE_PACKAGE + ".biz." + group.toLowerCase();
        String ioPrefix = (clientSystemCode != null && !clientSystemCode.isBlank()) ? clientSystemCode.toUpperCase() : interfaceId;
        String mciGroupPath = "infra/itrf/mci/" + group.toLowerCase();
        String clientPrefixCap = "";
        Path mciClientDir = null;

        if (isMci && clientSystemCode != null && (clientSystemCode.length() == 4 || clientSystemCode.length() == 9)) {
            String clientPrefix = clientSystemCode.length() == 9 ? clientSystemCode.substring(1, 5).toLowerCase() : clientSystemCode.toLowerCase();
            clientPrefixCap = toPascalCase(clientPrefix);
            mciGroupPath = "infra/itrf/mci/" + clientPrefix.substring(0, 3) + "/" + clientPrefix.substring(3);
            mciClientDir = rootDir.resolve(Paths.get(moduleName, BASE_PACKAGE_PATH, mciGroupPath));
        }

        String converterPackage = bizPackage + ".converter";
        String targetSystemPackage = isMci ? mciTargetSystemPackage(clientSystemCode) : null;
        if (targetSystemPackage != null) {
            converterPackage += "." + targetSystemPackage;
        }
        String converterClassName = (isMci && clientSystemCode != null && !clientSystemCode.isBlank())
                ? clientSystemCode.toUpperCase(Locale.ROOT) + "Converter"
                : baseName + "Converter";
        Path converterDir = rootDir.resolve(Paths.get(moduleName, BASE_PACKAGE_PATH, "biz", group.toLowerCase(), "converter"));
        if (targetSystemPackage != null) {
            for (String segment : targetSystemPackage.split("\\.")) {
                converterDir = converterDir.resolve(segment);
            }
        }

        Path mciIoDir = rootDir.resolve(Paths.get(moduleName, BASE_PACKAGE_PATH, mciGroupPath, "io"));
        String httpApiPackage = toPackageSegment(httpApiName);
        String httpApiClass = toPascalCase(httpApiName);
        String httpGroupPath = "infra/itrf/http/" + httpApiPackage;
        Path httpClientDir = rootDir.resolve(Paths.get(moduleName, BASE_PACKAGE_PATH, httpGroupPath));
        Path httpIoDir = httpClientDir.resolve("io");

        Files.createDirectories(usecaseDir);
        Files.createDirectories(usecaseImplDir);
        Files.createDirectories(dtoDir);
        if (isMci) {
            Files.createDirectories(mciIoDir);
            if (mciClientDir != null) {
                Files.createDirectories(mciClientDir);
            }
        } else if (isHttp) {
            Files.createDirectories(httpIoDir);
        } else {
            Files.createDirectories(legacyDtoDir);
        }
        Files.createDirectories(converterDir);

        StringBuilder log = new StringBuilder();

        // Generate Request DTO
        String reqContent = """
            package %s.dto;

            import com.fasterxml.jackson.annotation.JsonInclude;
            import lombok.Data;

            /**
             * @package %s.dto
             * @className %sRequest
             * @description AX HUB 시스템 처리 클래스
             * @author %s
             * @create %s
             * <pre>
             * ---------- 개정이력 ----------
             * 수정일       수정자     수정내용
             * ---------- -------- ---------------------------
             * %s  %s    최초생성
             * 
             * </pre>
             */
            @Data
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public class %sRequest {
                @McpToolParam(description = "수신자 전화번호", required = true)
                -(?:\\\\d{3}|\\\\d{4})-\\\\d{4}$", examples = {"010-1234-5678"})
                private String phoneNumber;

                @McpToolParam(description = "전송할 메시지 내용", required = true)
                private String message;
            }
            """.formatted(bizPackage, bizPackage, baseName, author, createDate, createDate, author, baseName);
        reqContent = reqContent
                .replace("import com.fasterxml.jackson.annotation.JsonInclude;",
                        "import com.fasterxml.jackson.annotation.JsonInclude;\nimport io.swagger.v3.oas.annotations.media.Schema;")
                .replaceAll("(?m)^\\s*@McpToolParam\\([^\\r\\n]*\\)\\R", "")
                .replaceAll("(?m)^\\s*-\\(\\?:[^\\r\\n]*\\R", "")
                .replace("private String phoneNumber;", "@Schema(example = \"01012345678\")\n    private String phoneNumber;")
                .replace("private String message;", "@Schema(example = \"테스트 메시지입니다.\")\n    private String message;");
        reqContent = dtoContent(bizPackage + ".dto", baseName + "Request", inputFields, author, createDate, true);
        writeUtf8(dtoDir.resolve(baseName + "Request.java"), reqContent);
        writeStructuredFieldTypes(dtoDir, bizPackage + ".dto", baseName + "Request", inputFields);

        // Generate Response DTO
        String resContent = """
            package %s.dto;

            import com.fasterxml.jackson.annotation.JsonInclude;
                                    import lombok.Data;

            /**
             * @package %s.dto
             * @className %sResponse
             * @description AX HUB 시스템 처리 클래스
             * @author %s
             * @create %s
             * <pre>
             * ---------- 개정이력 ----------
             * 수정일       수정자     수정내용
             * ---------- -------- ---------------------------
             * %s  %s    최초생성
             * 
             * </pre>
             */
            @Data
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public class %sResponse {
                private String resultCode;

                private String resultMessage;

                // TODO: Add response fields here. Do not include PII in the Tool response.
            }
            """.formatted(bizPackage, bizPackage, baseName, author, createDate, createDate, author, baseName);
        resContent = dtoContent(bizPackage + ".dto", baseName + "Response", outputFields, author, createDate, false);
        writeUtf8(dtoDir.resolve(baseName + "Response.java"), resContent);
        writeStructuredFieldTypes(dtoDir, bizPackage + ".dto", baseName + "Response", outputFields);

        String toolName = toToolName(moduleName, group, toolBaseName);

        boolean isMutation = isMutationTool(toolBaseName);
        StringBuilder sb = new StringBuilder("    @GrowToolHint(\n");
        if (useSchemaResource) {
            sb.append("        inputSchemaResource = \"").append(inputSchemaClasspath).append("\",\n");
            sb.append("        outputSchemaResource = \"").append(outputSchemaClasspath).append("\",\n");
        }
        sb.append("        requiresApproval = ").append(isMutation).append(",\n");
        sb.append("        categoryKey = \"").append(group.toLowerCase(Locale.ROOT)).append("\",\n");
        sb.append("        timeoutMillis = ").append(definitionOptions.timeoutMillisOrDefault()).append("L,\n");
        sb.append("        retryMaxAttempts = ").append(definitionOptions.retryMaxAttemptsOrDefault()).append(",\n");
        if (interfaceId != null && !interfaceId.isBlank()) {
            sb.append("        mappingId = \"").append(interfaceId).append("\",\n");
        }
        sb.append("        functionDescription = \"").append(javaText(definitionOptions.functionDescription() != null && !definitionOptions.functionDescription().isBlank() ? definitionOptions.functionDescription() : title + " 기능을 수행합니다.")).append("\",\n");
        sb.append("        whenToUse = \"").append(javaText(definitionOptions.whenToUse() != null && !definitionOptions.whenToUse().isBlank() ? definitionOptions.whenToUse() : "사용자가 이 업무 기능의 실행 또는 조회를 요청할 때 사용합니다.")).append("\",\n");
        sb.append("        whenNotToUse = \"").append(javaText(definitionOptions.whenNotToUse() != null && !definitionOptions.whenNotToUse().isBlank() ? definitionOptions.whenNotToUse() : "정보 변경이나 실행 작업에는 사용하지 않습니다.")).append("\",\n");
        sb.append("        ioLimits = \"").append(javaText(definitionOptions.ioLimits() != null && !definitionOptions.ioLimits().isBlank() ? definitionOptions.ioLimits() : "정의된 입력 항목만 허용하며 업무 결과만 반환합니다.")).append("\",\n");
        sb.append("        displayDescription = \"").append(javaText(definitionOptions.displayDescription() != null && !definitionOptions.displayDescription().isBlank() ? definitionOptions.displayDescription() : title + " 정보를 처리합니다.")).append("\",\n");
        
        List<String> examples = definitionOptions.exampleQueries();
        if (examples == null || examples.isEmpty()) {
            examples = List.of(title + " 정보를 보여줘", title + " 확인해줘", "현재 " + title + " 알려줘");
        }
        sb.append("        exampleQueries = {")
          .append(examples.stream().map(q -> "\"" + javaText(q) + "\"").collect(Collectors.joining(", ")))
          .append("},\n");
          
        sb.append("        destructive = ").append(isMutation).append(",\n");
        sb.append("        idempotent = ").append(!isMutation).append(",\n");
        
        List<String> tags = definitionOptions.tags();
        if (tags == null || tags.isEmpty()) {
            tags = List.of(group.toLowerCase(Locale.ROOT), isMutation ? "처리" : "조회");
        }
        sb.append("        tags = {")
          .append(tags.stream().map(t -> "\"" + javaText(t) + "\"").collect(Collectors.joining(", ")))
          .append("},\n");
          
        sb.append("        ownerOrg = \"").append(javaText(definitionOptions.ownerOrg() != null && !definitionOptions.ownerOrg().isBlank() ? definitionOptions.ownerOrg() : "MCP_TOOL")).append("\"\n");
        sb.append("    )");
        String toolHintLine = sb.toString();

        String serviceInterfaceContent = """
            package %s.usecase;

            import org.springaicommunity.mcp.annotation.McpTool;
            import io.shinhanlife.dat.lib.annotation.GrowToolHint;
            import %s.dto.%sRequest;
            import %s.dto.%sResponse;

            /**
             * @package %s.usecase
             * @className %sUseCase
             * @description AX HUB 시스템 처리 클래스
             * @author %s
             * @create %s
             * <pre>
             * ---------- 개정이력 ----------
             * 수정일       수정자     수정내용
             * ---------- -------- ---------------------------
             * %s  %s    최초생성
             *
             * </pre>
             */
            public interface %sUseCase {

                @McpTool(name = "%s", title = "%s", description = "%s")
            %s
                %sResponse execute(%sRequest req);
            }
            """.formatted(
                bizPackage,
                bizPackage, baseName,
                bizPackage, baseName,
                bizPackage, baseName, author, createDate, createDate, author,
                baseName,
                toolName, title, description,
                toolHintLine,
                baseName, baseName
        );

        String methodName = Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1);
        serviceInterfaceContent = serviceInterfaceContent.replace("execute(", methodName + "(");
        writeUtf8(usecaseDir.resolve(baseName + "UseCase.java"), serviceInterfaceContent);

        String serviceImplContent;

        if (isMci) {
            serviceImplContent = """
                package %s.usecase.impl;

                import %s.dto.%sRequest;
                import %s.dto.%sResponse;
                import %s.usecase.%sUseCase;
                import io.shinhanlife.dat.lib.integration.mci.component.AxhubMciComponent;
                import io.shinhanlife.glow.communication.dto.Transfer;
                import io.shinhanlife.glow.BizException;
                import org.springframework.stereotype.Service;
                import lombok.RequiredArgsConstructor;
                import lombok.extern.slf4j.Slf4j;
                import %s.%s;
                import %s.%s.io.%s_I;
                import %s.%s.io.%s_O;
                %s

                /**
                 * @package %s.usecase.impl
                 * @className %sUseCaseImpl
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
                @Slf4j
                @Service
                @RequiredArgsConstructor
                public class %sUseCaseImpl implements %sUseCase {

                    %s
                    private final %s converter;

                    @Override
                    public %sResponse execute(%sRequest req) {
                        log.info("[MCI Tool] {} 요청 수신.", "%s");
                        try {
                            // MapStruct를 이용한 자동 매핑 (AI DTO -> MCI DTO)
                            %s_I mciReq = converter.toLegacyRequest(req);
                            Transfer<%s_O> resTransfer = null;

                            if (mciReq != null) {
                                resTransfer = mci.callTo(
                                        "%s",
                                        "%s",
                                        mciReq,
                                        %s_O.class
                                );
                            }

                            %sResponse response = new %sResponse();
                            if (resTransfer != null && resTransfer.getBody() != null) {
                                response = converter.toResponse(resTransfer.getBody());
                            }

                            response.setResultCode("SUCCESS");
                            response.setResultMessage(resTransfer != null && resTransfer.getBody() != null
                                    ? "MCI call completed."
                                    : "MCI call completed without a response body.");
                            return response;
                        } catch (BizException e) {
                            log.error("[MCI Tool] 연동 중 오류 발생: {}", e.getMessage(), e);
                            %sResponse response = new %sResponse();
                            response.setResultCode("ERROR");
                            response.setResultMessage(e.getMessage() != null ? e.getMessage() : "Unknown error");
                            return response;
                        } catch (Exception e) {
                            log.error("[MCI Tool] 연동 중 오류 발생: {}", e.getMessage(), e);
                            %sResponse response = new %sResponse();
                            response.setResultCode("ERROR");
                            response.setResultMessage(e.getMessage() != null ? e.getMessage() : "Unknown error");
                            return response;
                        }
                    }
                }
                """.formatted(
                    bizPackage,
                    bizPackage, baseName,
                    bizPackage, baseName,
                    bizPackage, baseName,
                    converterPackage, converterClassName,
                    BASE_PACKAGE, mciGroupPath.replace("/", "."), ioPrefix,
                    BASE_PACKAGE, mciGroupPath.replace("/", "."), ioPrefix,
                    (clientPrefixCap.isEmpty() ? "" : "import " + BASE_PACKAGE + "." + mciGroupPath.replace("/", ".") + ".Mci" + clientPrefixCap + "Client;\n"),
                    bizPackage,
                    baseName,
                    author,
                    createDate,
                    createDate, author,
                    baseName,
                    baseName,
                    (clientPrefixCap.isEmpty() ? "private final AxhubMciComponent mci;" : "private final Mci" + clientPrefixCap + "Client mci;"),
                    converterClassName,
                    baseName,
                    baseName,
                    toolName,
                    ioPrefix,
                    ioPrefix,
                    interfaceId,
                    ((clientSystemCode != null && !clientSystemCode.isBlank()) ? clientSystemCode.toUpperCase() : ""),
                    ioPrefix,
                    baseName,
                    baseName,
                    baseName,
                    baseName,
                    baseName,
                    baseName
            );
        } else {
            serviceImplContent = isHttp
                    ? httpUseCaseImplContent(bizPackage, baseName, httpGroupPath.replace("/", "."), httpApiClass, author, createDate)
                    : """
                package %s.usecase.impl;

                import %s.dto.%sRequest;
                import %s.dto.%sResponse;
                import %s.usecase.%sUseCase;
                import %s.converter.%sConverter;
                import org.springframework.stereotype.Service;
                import lombok.RequiredArgsConstructor;
                import lombok.extern.slf4j.Slf4j;

                /**
                 * @package %s.usecase.impl
                 * @className %sUseCaseImpl
                 * @description AX HUB 시스템 처리 클래스
                 * @author %s
                 * @create %s
                 * <pre>
                 * ---------- 개정이력 ----------
                 * 수정일       수정자     수정내용
                 * ---------- -------- ---------------------------
                 * %s  %s    최초생성
                 * 
                 * </pre>
                 */
                @Slf4j
                @Service
                @RequiredArgsConstructor
                public class %sUseCaseImpl implements %sUseCase {

                    private final %sConverter converter;

                    @Override
                    public %sResponse execute(%sRequest req) {
                        // %sLegacyRequest legacyRequest = converter.toLegacyRequest(req);
                        Object legacyResponse = null;
                        if (legacyResponse instanceof %sResponse response) {
                            return response;
                        }
                        %sResponse response = new %sResponse();
                        response.setResultCode("SUCCESS");
                        response.setResultMessage("Legacy call completed.");
                        return response;
                    }
                }
                """.formatted(
                    bizPackage,
                    bizPackage, baseName,
                    bizPackage, baseName,
                    bizPackage, baseName,
                    bizPackage, baseName,
                    bizPackage, baseName,
                    author,
                    createDate,
                    createDate, author,
                    baseName, baseName,
                    baseName,
                    baseName,
                    baseName,
                    baseName,
                    baseName,
                    baseName,
                    baseName,
                    routingType, interfaceId
            );
        }

        serviceImplContent = serviceImplContent.replace("execute(", methodName + "(");
        writeUtf8(usecaseImplDir.resolve(baseName + "UseCaseImpl.java"), serviceImplContent);

        if (isMci) {

            String mciReqContent = """
                package %s.%s.io;

                import lombok.Data;

                /**
                 * @package %s.%s.io
                 * @className %s_I
                 * @description AX HUB 시스템 처리 클래스
                 * @author %s
                 * @create %s
                 * <pre>
                 * ---------- 개정이력 ----------
                 * 수정일       수정자     수정내용
                 * ---------- -------- ---------------------------
                 * %s  %s    최초생성
                 * 
                 * </pre>
                 */
                @Data
                public class %s_I {
                    // TODO: Add request fields here
                    /**
                     * EAI 시스템이 요구하는 수신자 번호 파라미터명
                     */
                    private String phone;

                    /**
                     * EAI 시스템이 요구하는 메시지 내용 파라미터명
                     */
                    private String content;
                }
                """.formatted(BASE_PACKAGE, mciGroupPath.replace("/", "."), BASE_PACKAGE, mciGroupPath.replace("/", "."), ioPrefix, author, createDate, createDate, author, ioPrefix);
            mciReqContent = mciIoContent(mciGroupPath.replace("/", "."), ioPrefix + "_I", inputFields, author, createDate);
            writeUtf8(mciIoDir.resolve(ioPrefix + "_I.java"), mciReqContent);
            writeStructuredFieldTypes(mciIoDir, BASE_PACKAGE + "." + mciGroupPath.replace("/", ".") + ".io", ioPrefix + "_I", inputFields);

            String mciResContent = """
                package %s.%s.io;

                import lombok.Data;

                /**
                 * @package %s.%s.io
                 * @className %s_O
                 * @description AX HUB 시스템 처리 클래스
                 * @author %s
                 * @create %s
                 * <pre>
                 * ---------- 개정이력 ----------
                 * 수정일       수정자     수정내용
                 * ---------- -------- ---------------------------
                 * %s  %s    최초생성
                 * 
                 * </pre>
                 */
                @Data
                public class %s_O {
                    // TODO: Add response fields here
                }
                """.formatted(BASE_PACKAGE, mciGroupPath.replace("/", "."), BASE_PACKAGE, mciGroupPath.replace("/", "."), ioPrefix, author, createDate, createDate, author, ioPrefix);
            mciResContent = mciIoContent(mciGroupPath.replace("/", "."), ioPrefix + "_O", outputFields, author, createDate);
            writeUtf8(mciIoDir.resolve(ioPrefix + "_O.java"), mciResContent);
            writeStructuredFieldTypes(mciIoDir, BASE_PACKAGE + "." + mciGroupPath.replace("/", ".") + ".io", ioPrefix + "_O", outputFields);

            String converterContent = """
                package %s.converter;

                import %s.dto.%sRequest;
                import %s.dto.%sResponse;
                import %s.%s.io.%s_I;
                import %s.%s.io.%s_O;
                import org.mapstruct.Mapper;
                import org.mapstruct.Mapping;
                import org.mapstruct.factory.Mappers;

                /**
                 * @package %s.converter
                 * @className %sConverter
                 * @description AX HUB 시스템 처리 클래스
                 * @author %s
                 * @create %s
                 * <pre>
                 * ---------- 개정이력 ----------
                 * 수정일       수정자     수정내용
                 * ---------- -------- ---------------------------
                 * %s  %s    최초생성
                 * 
                 * </pre>
                 */
                @Mapper(componentModel = "spring")
                public interface %sConverter {

                    @Mapping(source = "phoneNumber", target = "phone")
                    @Mapping(source = "message", target = "content")
                    %s_I toLegacyRequest(%sRequest req);
                    
                    @Mapping(source = "phone", target = "phoneNumber")
                    @Mapping(source = "content", target = "message")
                    %sRequest toRequest(%s_I mciReq);

                    // %sResponse toResponse(%s_O mciRes);
                }
                """.formatted(
                    bizPackage,
                    bizPackage, baseName,
                    bizPackage, baseName,
                    BASE_PACKAGE, mciGroupPath.replace("/", "."), ioPrefix,
                    BASE_PACKAGE, mciGroupPath.replace("/", "."), ioPrefix,
                    bizPackage, baseName, author, createDate, createDate, author,
                    baseName, ioPrefix, baseName,
                    baseName, ioPrefix,
                    baseName, ioPrefix
            );
            converterContent = mciConverterContent(converterPackage, bizPackage, baseName, mciGroupPath.replace("/", "."), ioPrefix, converterClassName);
            writeUtf8(converterDir.resolve(converterClassName + ".java"), converterContent);

            log.append("\n=========================================\n");
            log.append(" Scaffolding Complete! (Routing: " + routingType + ")\n");
            log.append("=========================================\n");
            log.append("[Usecase Interface] ").append(usecaseDir.resolve(baseName + "UseCase.java")).append("\n");
            log.append("[Usecase Impl] ").append(usecaseImplDir.resolve(baseName + "UseCaseImpl.java")).append("\n");
            log.append("[Request DTO] ").append(dtoDir.resolve(baseName + "Request.java")).append("\n");
            log.append("[Response DTO] ").append(dtoDir.resolve(baseName + "Response.java")).append("\n");
            log.append("[MCI Request IO] ").append(mciIoDir.resolve(interfaceId + "_I.java")).append("\n");
            log.append("[MCI Response IO] ").append(mciIoDir.resolve(interfaceId + "_O.java")).append("\n");
            log.append("[MCI Converter] ").append(converterDir.resolve(converterClassName + ".java")).append("\n");

            if (!clientPrefixCap.isEmpty()) {
                String receiveServiceId = (clientSystemCode != null && !clientSystemCode.isBlank()) ? clientSystemCode.toUpperCase() : "";
                String mciClientContent = """
                    package %s.%s;

                    import org.springframework.stereotype.Component;
                    import lombok.RequiredArgsConstructor;
                    import io.shinhanlife.dat.lib.integration.mci.component.AxhubMciComponent;
                    import io.shinhanlife.glow.communication.dto.Transfer;
                    import %s.%s.io.%s_I;
                    import %s.%s.io.%s_O;

                    /**
                     * @package %s.%s
                     * @className Mci%sClient
                     * @description AX HUB 시스템 처리 클래스
                     * @author %s
                     * @create %s
                     * <pre>
                     * ---------- 개정이력 ----------
                     * 수정일       수정자     수정내용
                     * ---------- -------- ---------------------------
                     * %s  %s    최초생성
                     *
                     * </pre>
                     */
                    @Component
                    @RequiredArgsConstructor
                    public class Mci%sClient {

                        private static final String INTERFACE_ID = "%s";
                        private static final String RECEIVE_SERVICE_ID = "%s";

                        private final AxhubMciComponent mciComponent;

                        public <O> Transfer<O> callTo(String interfaceId, String receiveServiceId, Object mciReq, Class<O> resType) {
                            return mciComponent.callTo(interfaceId, receiveServiceId, mciReq, resType);
                        }

                        public %s_O call%s(%s_I request) {
                            Transfer<%s_O> transfer = mciComponent.callTo(
                                    INTERFACE_ID, RECEIVE_SERVICE_ID, request, %s_O.class);
                            return transfer == null ? null : transfer.getBody();
                        }
                    }
                    """.formatted(
                        BASE_PACKAGE, mciGroupPath.replace("/", "."),
                        BASE_PACKAGE, mciGroupPath.replace("/", "."), ioPrefix,
                        BASE_PACKAGE, mciGroupPath.replace("/", "."), ioPrefix,
                        BASE_PACKAGE, mciGroupPath.replace("/", "."), clientPrefixCap, author, createDate, createDate, author,
                        clientPrefixCap,
                        interfaceId, receiveServiceId,
                        ioPrefix, toPascalCase(ioPrefix), ioPrefix,
                        ioPrefix, ioPrefix
                );
                writeUtf8(mciClientDir.resolve("Mci" + clientPrefixCap + "Client.java"), mciClientContent);
                log.append("[MCI Client] ").append(mciClientDir.resolve("Mci" + clientPrefixCap + "Client.java")).append("\n");
            }

        } else if (isHttp) {
            String httpPackage = BASE_PACKAGE + "." + httpGroupPath.replace("/", ".");
            String httpRequestClass = baseName + "HttpRequest";
            String httpResponseClass = baseName + "HttpResponse";
            String httpClientClass = httpApiClass + "Client";

            writeUtf8(httpIoDir.resolve(httpRequestClass + ".java"),
                    dtoContent(httpPackage + ".io", httpRequestClass, inputFields, author, createDate, true));
            writeUtf8(httpIoDir.resolve(httpResponseClass + ".java"),
                    dtoContent(httpPackage + ".io", httpResponseClass, outputFields, author, createDate, false));
            writeStructuredFieldTypes(httpIoDir, httpPackage + ".io", httpRequestClass, inputFields);
            writeStructuredFieldTypes(httpIoDir, httpPackage + ".io", httpResponseClass, outputFields);
            writeUtf8(httpClientDir.resolve(httpClientClass + ".java"),
                    httpClientContent(httpPackage, httpClientClass, httpApiName));
            writeUtf8(converterDir.resolve(baseName + "Converter.java"),
                    httpConverterContent(bizPackage, baseName, httpPackage));

            log.append("\n=========================================\n");
            log.append(" Scaffolding Complete! (Routing: HTTP)\n");
            log.append("=========================================\n");
            log.append("[Usecase Interface] ").append(usecaseDir.resolve(baseName + "UseCase.java")).append("\n");
            log.append("[Usecase Impl] ").append(usecaseImplDir.resolve(baseName + "UseCaseImpl.java")).append("\n");
            log.append("[Request DTO] ").append(dtoDir.resolve(baseName + "Request.java")).append("\n");
            log.append("[Response DTO] ").append(dtoDir.resolve(baseName + "Response.java")).append("\n");
            log.append("[HTTP Request IO] ").append(httpIoDir.resolve(httpRequestClass + ".java")).append("\n");
            log.append("[HTTP Response IO] ").append(httpIoDir.resolve(httpResponseClass + ".java")).append("\n");
            log.append("[HTTP Client] ").append(httpClientDir.resolve(httpClientClass + ".java")).append("\n");
            log.append("[HTTP Converter] ").append(converterDir.resolve(baseName + "Converter.java")).append("\n");
        } else {
            String legacyReqContent = """
                package %s.legacy;

                import lombok.Data;

                /**
                 * @package %s.legacy
                 * @className %sLegacyRequest
                 * @description AX HUB 시스템 처리 클래스
                 * @author %s
                 * @create %s
                 * <pre>
                 * ---------- 개정이력 ----------
                 * 수정일       수정자     수정내용
                 * ---------- -------- ---------------------------
                 * %s  %s    최초생성
                 * 
                 * </pre>
                 */
                @Data
                public class %sLegacyRequest {
                    /**
                     * EAI 시스템이 요구하는 수신자 번호 파라미터명
                     */
                    private String phone;

                    /**
                     * EAI 시스템이 요구하는 메시지 내용 파라미터명
                     */
                    private String content;
                }
                """.formatted(bizPackage, bizPackage, baseName, author, createDate, createDate, author, baseName);
            legacyReqContent = dtoContent(bizPackage + ".legacy", baseName + "LegacyRequest", inputFields, author, createDate, true);
            writeUtf8(legacyDtoDir.resolve(baseName + "LegacyRequest.java"), legacyReqContent);

            String legacyResContent = """
                package %s.legacy;

                import lombok.Data;

                /**
                 * @package %s.legacy
                 * @className %sLegacyResponse
                 * @description AX HUB 시스템 처리 클래스
                 * @author %s
                 * @create %s
                 * <pre>
                 * ---------- 개정이력 ----------
                 * 수정일       수정자     수정내용
                 * ---------- -------- ---------------------------
                 * %s  %s    최초생성
                 * 
                 * </pre>
                 */
                @Data
                public class %sLegacyResponse {
                    // TODO: Add legacy response fields here
                }
                """.formatted(bizPackage, bizPackage, baseName, author, createDate, createDate, author, baseName);
            legacyResContent = dtoContent(bizPackage + ".legacy", baseName + "LegacyResponse", outputFields, author, createDate, true);
            writeUtf8(legacyDtoDir.resolve(baseName + "LegacyResponse.java"), legacyResContent);

            String converterContent = """
                package %s.converter;

                import %s.dto.%sRequest;
                import %s.dto.%sResponse;
                import %s.legacy.%sLegacyRequest;
                import %s.legacy.%sLegacyResponse;
                import org.mapstruct.Mapper;
                import org.mapstruct.Mapping;
                import org.mapstruct.factory.Mappers;

                /**
                 * @package %s.converter
                 * @className %sConverter
                 * @description AX HUB 시스템 처리 클래스
                 * @author %s
                 * @create %s
                 * <pre>
                 * ---------- 개정이력 ----------
                 * 수정일       수정자     수정내용
                 * ---------- -------- ---------------------------
                 * %s  %s    최초생성
                 * 
                 * </pre>
                 */
                @Mapper(componentModel = "spring")
                public interface %sConverter {

                    @Mapping(source = "phoneNumber", target = "phone")
                    @Mapping(source = "message", target = "content")
                    %sLegacyRequest toLegacyRequest(%sRequest req);
                    
                    @Mapping(source = "phone", target = "phoneNumber")
                    @Mapping(source = "content", target = "message")
                    %sRequest toRequest(%sLegacyRequest legacyRequest);

                    // %sResponse toResponse(%sLegacyResponse legacyResponse);
                }
                """.formatted(
                    bizPackage,
                    bizPackage, baseName,
                    bizPackage, baseName,
                    bizPackage, baseName,
                    bizPackage, baseName,
                    bizPackage, baseName, author, createDate, createDate, author,
                    baseName, baseName, baseName, baseName, baseName, baseName, baseName
            );
            converterContent = legacyConverterContent(bizPackage, baseName);
            writeUtf8(converterDir.resolve(baseName + "Converter.java"), converterContent);

            log.append("\n=========================================\n");
            log.append(" Scaffolding Complete! (Routing: " + routingType + ")\n");
            log.append("=========================================\n");
            log.append("[Usecase Interface] ").append(usecaseDir.resolve(baseName + "UseCase.java")).append("\n");
            log.append("[Usecase Impl] ").append(usecaseImplDir.resolve(baseName + "UseCaseImpl.java")).append("\n");
            log.append("[Request DTO] ").append(dtoDir.resolve(baseName + "Request.java")).append("\n");
            log.append("[Response DTO] ").append(dtoDir.resolve(baseName + "Response.java")).append("\n");
            log.append("[Legacy Request DTO] ").append(legacyDtoDir.resolve(baseName + "LegacyRequest.java")).append("\n");
            log.append("[Legacy Response DTO] ").append(legacyDtoDir.resolve(baseName + "LegacyResponse.java")).append("\n");
            log.append("[Legacy Converter] ").append(converterDir.resolve(baseName + "Converter.java")).append("\n");
        }
        // Schema Resource 파일 생성(useSchemaResource=true일 때)
        if (useSchemaResource) {
            Files.createDirectories(schemaDir);
            String inputSchema = """
                {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "TODO_FIELD": {
                      "type": "string",
                      "description": "TODO: 파라미터 설명을 입력하세요."
                    }
                  },
                  "required": []
                }
                """;
            String outputSchema = """
                {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "status": {
                      "type": "string",
                      "description": "처리 결과 상태 (SUCCESS / FAILURE)",
                      "enum": ["SUCCESS", "FAILURE"]
                    },
                    "message": {
                      "type": "string",
                      "description": "처리 결과 메시지"
                    }
                  },
                  "required": ["status"]
                }
                """;
            writeUtf8(schemaDir.resolve(inputSchemaFileName), inputSchema);
            writeUtf8(schemaDir.resolve(outputSchemaFileName), outputSchema);
            log.append("[Input  Schema] ").append(schemaDir.resolve(inputSchemaFileName)).append("\n");
            log.append("[Output Schema] ").append(schemaDir.resolve(outputSchemaFileName)).append("\n");
        }

        // Response JSON mock files are intentionally not generated. Runtime response contracts are represented by DTOs.
        if (isHttp) {
            Path moduleRoot = rootDir.resolve(moduleName).toAbsolutePath().normalize();
            Path projectRoot = moduleRoot.getParent();
            Path glowConfig = ensureLocalHttpApiConfiguration(projectRoot, httpApiName, toolName);
            log.append("[HTTP Config] ").append(glowConfig).append("\n");
        }

        Path generatedTestDir = rootDir.resolve(Paths.get(moduleName, "src/test/java/io/shinhanlife/dat/mcc/biz", group.toLowerCase(), "usecase"));
        Files.createDirectories(generatedTestDir);
        Path generatedTestPath = generatedTestDir.resolve(baseName + "UseCaseTest.java");
        writeUtf8(generatedTestPath, useCaseTestContent(bizPackage, baseName));
        log.append("[Unit Test] ").append(generatedTestPath).append("\\n");
        log.append("[Test Command] .\\gradlew.bat :").append(moduleName.substring(moduleName.lastIndexOf(java.io.File.separator) + 1)).append(":test --tests \"*").append(baseName).append("UseCaseTest\"\\n");
        // Tool Definition YML is no longer generated. We use @GrowToolHint instead.
        // log.append("[V17 Tool Definition] ...\n");
        log.append("\n Tip: HTTP Tool은 WireMock 실행 후 생성된 mapping URL로 호출을 확인하세요.\n");

        return log.toString();
    }

    private static String toolDefinitionContent(String toolName, String title, String description,
                                                String categoryKey, String interfaceId,
                                                List<FieldDefinition> inputFields, boolean mutation) {
        String safeDescription = description == null || description.isBlank()
                ? title + " 기능을 수행한다." : description;
        StringBuilder properties = new StringBuilder();
        StringBuilder required = new StringBuilder();
        Set<String> generatedNames = new LinkedHashSet<>();
        for (FieldDefinition field : inputFields == null ? List.<FieldDefinition>of() : inputFields) {
            if (field == null || field.name() == null || field.name().isBlank()
                    || !generatedNames.add(field.name().trim())) {
                continue;
            }
            appendSchemaProperty(properties, field);
            if (field.required()) {
                required.append("    - ").append(field.name()).append("\n");
            }
        }
        if (properties.isEmpty()) {
            properties.append("    {}\n");
        }
        String requiredBlock = required.isEmpty() ? "" : "  required:\n" + required;
        String legacyLine = interfaceId == null || interfaceId.isBlank()
                ? "" : "legacy_interface_id: " + yamlText(interfaceId) + "\n";
        return """
                name: %s
                display_name: %s
                version: 1.0.0
                category_key: %s
                description:
                  function: %s
                  when_to_use: 사용자가 이 업무 기능의 실행 또는 조회를 명확히 요청한 경우 사용한다.
                  when_not_to_use: 입력값이 확인되지 않았거나 다른 업무 기능이 더 적합한 경우에는 사용하지 않는다.
                  io_limits: 정의된 입력 항목만 허용하며 응답 DTO에 정의된 업무 결과만 반환한다.
                display_description: %s
                example_queries:
                  - %s 처리해줘
                  - %s 정보를 확인해줘
                  - %s 업무 결과를 알려줘
                read_only: %s
                destructive: %s
                idempotent: %s
                parameters_schema:
                  type: object
                  properties:
                %s%s  additionalProperties: false
                tags: [%s]
                %srequired_env_keys: []
                owner_org: MCP_TOOL
                """.formatted(toolName, yamlText(title), categoryKey.toLowerCase(Locale.ROOT),
                yamlText(safeDescription), yamlText(title), yamlText(title), yamlText(title), yamlText(title),
                !mutation, mutation, !mutation, properties, requiredBlock,
                categoryKey.toLowerCase(Locale.ROOT), legacyLine);
    }

    private static String toolDefinitionContentV17(String toolName, String title, String description,
                                                   String categoryKey, String interfaceId,
                                                   List<FieldDefinition> inputFields, boolean mutation,
                                                   ToolDefinitionOptions options) {
        String function = option(options == null ? null : options.functionDescription(),
                option(description, title + " 기능을 수행한다."));
        String whenToUse = option(options == null ? null : options.whenToUse(),
                "사용자가 해당 업무 기능의 실행 또는 조회를 명확히 요청한 경우 사용한다.");
        String whenNotToUse = option(options == null ? null : options.whenNotToUse(),
                "필수 입력값이 확인되지 않았거나 다른 업무 기능이 더 적합한 경우에는 사용하지 않는다.");
        String ioLimits = option(options == null ? null : options.ioLimits(),
                "정의된 입력 항목만 허용하며 응답 DTO에 정의된 업무 결과만 반환한다.");
        String displayDescription = option(options == null ? null : options.displayDescription(), title);
        List<String> examples = normalizedList(options == null ? null : options.exampleQueries(), List.of(
                title + " 처리해줘", title + " 정보를 확인해줘", title + " 업무 결과를 알려줘"));
        List<String> tags = normalizedList(options == null ? null : options.tags(),
                List.of(categoryKey.toLowerCase(Locale.ROOT)));
        String ownerOrg = option(options == null ? null : options.ownerOrg(), "MCP_TOOL");

        StringBuilder properties = new StringBuilder();
        StringBuilder required = new StringBuilder();
        Set<String> generatedNames = new LinkedHashSet<>();
        for (FieldDefinition field : inputFields == null ? List.<FieldDefinition>of() : inputFields) {
            if (field == null || field.name() == null || field.name().isBlank()
                    || !generatedNames.add(field.name().trim())) {
                continue;
            }
            appendSchemaProperty(properties, field);
            if (field.required()) {
                required.append("    - ").append(field.name().trim()).append("\n");
            }
        }
        if (properties.isEmpty()) {
            properties.append("    {}\n");
        }
        String requiredBlock = required.isEmpty() ? "" : "  required:\n" + required;
        String legacyLine = interfaceId == null || interfaceId.isBlank()
                ? "" : "legacy_interface_id: " + yamlText(interfaceId) + "\n";
        String exampleBlock = examples.stream().map(value -> "  - " + yamlText(value))
                .collect(java.util.stream.Collectors.joining("\n"));
        String tagBlock = tags.stream().map(ToolScaffolder::yamlText)
                .collect(java.util.stream.Collectors.joining(", "));

        return """
                name: %s
                display_name: %s
                version: 1.0.0
                category_key: %s
                description:
                  function: %s
                  when_to_use: %s
                  when_not_to_use: %s
                  io_limits: %s
                display_description: %s
                example_queries:
                %s
                read_only: %s
                destructive: %s
                idempotent: %s
                parameters_schema:
                  type: object
                  properties:
                %s%s  additionalProperties: false
                tags: [%s]
                %srequired_env_keys: []
                owner_org: %s
                """.formatted(toolName, yamlText(title), categoryKey.toLowerCase(Locale.ROOT),
                yamlText(function), yamlText(whenToUse), yamlText(whenNotToUse), yamlText(ioLimits),
                yamlText(displayDescription), exampleBlock, !mutation, mutation, !mutation,
                properties, requiredBlock, tagBlock, legacyLine, yamlText(ownerOrg));
    }

    private static String option(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static List<String> normalizedList(List<String> values, List<String> fallback) {
        if (values == null) {
            return fallback;
        }
        List<String> normalized = values.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static boolean isMutationTool(String baseName) {
        String value = baseName.toLowerCase(Locale.ROOT);
        return value.matches(".*(create|add|update|delete|remove|send|process|approve|reject|register|issue).*");
    }

    private static String jsonSchemaType(String javaType) {
        return switch (javaType == null ? "String" : javaType) {
            case "Integer", "Long" -> "integer";
            case "Double", "BigDecimal" -> "number";
            case "Boolean" -> "boolean";
            case "List" -> "array";
            default -> "string";
        };
    }

    private static void appendSchemaProperty(StringBuilder properties, FieldDefinition field) {
        properties.append("    ").append(field.name().trim()).append(":\n")
                .append("      type: ").append(jsonSchemaType(field.type())).append("\n")
                .append("      description: ").append(yamlText(richDescription(field))).append("\n");
        if ("Enum".equals(field.type()) && field.enumValues() != null && !field.enumValues().isEmpty()) {
            properties.append("      enum: [").append(field.enumValues().stream()
                    .filter(value -> value != null && !value.isBlank()).map(String::trim)
                    .collect(java.util.stream.Collectors.joining(", "))).append("]\n");
        }
        if ("List".equals(field.type())) {
            properties.append("      items:\n")
                    .append("        type: ").append("Object".equals(field.itemType()) ? "object" : jsonSchemaType(field.itemType())).append("\n");
            if ("Object".equals(field.itemType()) && field.itemFields() != null && !field.itemFields().isEmpty()) {
                properties.append("        properties:\n");
                for (FieldDefinition itemField : field.itemFields()) {
                    properties.append("          ").append(itemField.name()).append(":\n")
                            .append("            type: ").append(jsonSchemaType(itemField.type())).append("\n")
                            .append("            description: ").append(yamlText(richDescription(itemField))).append("\n");
                }
            }
        }
    }

    private static String yamlText(String value) {
        String safe = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", " ").replace("\n", " ");
        return "\"" + safe + "\"";
    }

    private static String toKebabCase(String pascalCase) {
        if (pascalCase == null || pascalCase.isEmpty()) return pascalCase;
        return pascalCase
                .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .toLowerCase(Locale.ROOT);
    }

    private static Path findGlowLocalConfigPath(Path startPath) {
        String sourceDir = System.getProperty("AXHUB_SOURCE_DIR");
        if (sourceDir == null) {
            sourceDir = System.getenv("AXHUB_SOURCE_DIR");
        }

        List<Path> baseDirs = new ArrayList<>();
        if (startPath != null) {
            baseDirs.add(startPath);
            if (startPath.getParent() != null) {
                baseDirs.add(startPath.getParent());
            }
        }
        if (sourceDir != null && !sourceDir.isBlank()) {
            baseDirs.add(Paths.get(sourceDir.trim()));
        }
        baseDirs.add(Paths.get("."));

        String[] subPaths = {
            "dat-was-lib/src/main/resources/glow",
            "src/main/resources/glow"
        };
        String[] fileNames = {
            "application-glow-local.yml",
            "application-glow-local.yaml",
            "application-glow-local.xml"
        };

        // 1. 이미 존재하는 파일 우선 탐색 (dat-was-lib 우선)
        for (Path base : baseDirs) {
            for (String sub : subPaths) {
                for (String name : fileNames) {
                    Path candidate = base.resolve(sub).resolve(name).normalize();
                    if (Files.exists(candidate)) {
                        return candidate;
                    }
                }
            }
        }

        // 2. dat-was-lib 디렉토리가 존재하는 디렉토리 찾기
        for (Path base : baseDirs) {
            Path libDir = base.resolve("dat-was-lib");
            if (Files.isDirectory(libDir)) {
                return libDir.resolve("src/main/resources/glow/application-glow-local.yml").normalize();
            }
        }

        // 3. startPath 형제(sibling)로 dat-was-lib 찾기
        if (startPath != null) {
            Path libSibling = startPath.resolveSibling("dat-was-lib");
            if (Files.isDirectory(libSibling)) {
                return libSibling.resolve("src/main/resources/glow/application-glow-local.yml").normalize();
            }
        }

        // 4. Fallback
        if (sourceDir != null && !sourceDir.isBlank()) {
            return Paths.get(sourceDir.trim()).resolve("dat-was-lib/src/main/resources/glow/application-glow-local.yml").normalize();
        }
        return Paths.get("dat-was-lib/src/main/resources/glow/application-glow-local.yml").normalize();
    }

    private static Path ensureLocalHttpApiConfiguration(Path projectRoot, String httpApiName, String toolName) throws IOException {
        Path localConfigPath = findGlowLocalConfigPath(projectRoot);
        Files.createDirectories(localConfigPath.getParent());
        String existing = Files.exists(localConfigPath) ? Files.readString(localConfigPath, StandardCharsets.UTF_8) : "";
        if (java.util.regex.Pattern.compile("(?m)^\\s*-\\s+name:\\s*"
                + java.util.regex.Pattern.quote(httpApiName) + "\\s*$").matcher(existing).find()) {
            return localConfigPath;
        }
        String environmentKey = toPackageSegment(httpApiName).toUpperCase(Locale.ROOT).replace('-', '_');
        String apiEntry = """
                        - name: %s
                          domain: ${AXHUB_%s_HTTP_DOMAIN:http://localhost:${server.port}}
                          url: ${AXHUB_%s_HTTP_URL:/api/mock/http/%s}
                          method: POST
                          content-type: application/json;charset=UTF-8
                          biz-pod: false
                """.formatted(httpApiName, environmentKey, environmentKey, toolName).stripTrailing() + "\n";
        boolean isCrlf = existing.contains("\r\n");
        String formattedApiEntry = isCrlf ? apiEntry.replace("\n", "\r\n") : apiEntry;

        if (existing.isBlank()) {
            existing = """
                    spring:
                      config:
                        activate:
                          on-profile: local

                    glow:
                      communication:
                        http:
                          api-list:
                    """ + apiEntry;
            if (isCrlf) {
                existing = existing.replace("\n", "\r\n");
            }
        } else if (existing.contains("\r\n    mci:")) {
            existing = existing.replace("\r\n    mci:", "\r\n" + formattedApiEntry + "    mci:");
        } else if (existing.contains("\n    mci:")) {
            existing = existing.replace("\n    mci:", "\n" + formattedApiEntry + "    mci:");
        } else if (existing.contains("\r\naxhub:")) {
            existing = existing.replace("\r\naxhub:", "\r\n" + formattedApiEntry + "axhub:");
        } else if (existing.contains("\naxhub:")) {
            existing = existing.replace("\naxhub:", "\n" + formattedApiEntry + "axhub:");
        } else if (existing.contains("api-list:")) {
            existing += formattedApiEntry;
        } else {
            throw new IllegalStateException("application-glow-local.yml must define glow.communication.http.api-list");
        }
        if (!existing.contains("axhub:\n  mock:\n    http:\n      enabled: true")
                && !existing.contains("axhub:\r\n  mock:\r\n    http:\r\n      enabled: true")) {
            String mockConfig = """

                    axhub:
                      mock:
                        http:
                          enabled: true
                    """;
            existing += isCrlf ? mockConfig.replace("\n", "\r\n") : mockConfig;
        }
        writeUtf8(localConfigPath, existing);
        return localConfigPath;
    }

    private static void writeUtf8(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static String dtoContent(String packageName, String className, List<FieldDefinition> fields,
                                     String author, String createDate, boolean request) {
        String body = fieldLines(fields, request ? Set.of() : Set.of("resultCode", "resultMessage"), className);
        if (!request) {
            body = "    private String resultCode;\n\n    private String resultMessage;\n" + body;
        }
        String listImport = hasListField(fields) ? "import java.util.List;\n" : "";
        String patternImport = hasPatternField(fields) ? "import jakarta.validation.constraints.Pattern;\n" : "";
        return """
                package %s;

                import com.fasterxml.jackson.annotation.JsonInclude;
                import io.swagger.v3.oas.annotations.media.Schema;
                import lombok.Data;
                %s%s

                @Data
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public class %s {
                %s%s}
                """.formatted(packageName, listImport, patternImport, className, body, innerObjectListClasses(fields));
    }

    private static String mciIoContent(String packageSuffix, String className, List<FieldDefinition> fields,
                                       String author, String createDate) {
        String listImport = hasListField(fields) ? "import java.util.List;\n" : "";
        String patternImport = hasPatternField(fields) ? "import jakarta.validation.constraints.Pattern;\n" : "";
        return """
                package %s.%s.io;

                import io.swagger.v3.oas.annotations.media.Schema;
                import lombok.Data;
                %s%s

                @Data
                public class %s {
                %s%s}
                """.formatted(BASE_PACKAGE, packageSuffix, listImport, patternImport, className, fieldLines(fields, Set.of(), className),
                innerObjectListClasses(fields));
    }

    private static boolean hasListField(List<FieldDefinition> fields) {
        return fields != null && fields.stream().anyMatch(field -> field != null && "List".equals(field.type()));
    }

    private static boolean hasPatternField(List<FieldDefinition> fields) {
        if (fields == null) return false;
        return fields.stream().anyMatch(field -> {
            if (field == null) return false;
            if (field.pattern() != null && !field.pattern().isBlank()) return true;
            if ("List".equals(field.type()) && "Object".equals(field.itemType())) {
                return hasPatternField(field.itemFields());
            }
            return false;
        });
    }

    private static void writeStructuredFieldTypes(Path directory, String packageName, String ownerClass,
                                                  List<FieldDefinition> fields) throws IOException {
        for (FieldDefinition field : fields == null ? List.<FieldDefinition>of() : fields) {
            if (field == null || field.name() == null || field.name().isBlank()) {
                continue;
            }
            if ("Enum".equals(field.type())) {
                String enumName = toPascalCase(field.name());
                List<String> values = field.enumValues() == null ? List.of() : field.enumValues().stream()
                        .filter(value -> value != null && !value.isBlank()).map(String::trim).distinct().toList();
                if (values.isEmpty()) {
                    throw new IllegalArgumentException("Enum field needs at least one allowed value: " + field.name());
                }
                String constants = values.stream().map(value -> "    " + enumConstant(value) + "(\"" + javaText(value) + "\")")
                        .collect(java.util.stream.Collectors.joining(",\n"));
                writeUtf8(directory.resolve(enumName + ".java"), """
                        package %s;

                        import com.fasterxml.jackson.annotation.JsonCreator;
                        import com.fasterxml.jackson.annotation.JsonValue;

                        public enum %s {
                        %s;

                            private final String value;

                            %s(String value) {
                                this.value = value;
                            }

                            @JsonValue
                            public String getValue() {
                                return value;
                            }

                            @JsonCreator
                            public static %s fromValue(String value) {
                                for (%s candidate : values()) {
                                    if (candidate.value.equals(value)) return candidate;
                                }
                                throw new IllegalArgumentException("Unsupported value: " + value);
                            }
                        }
                        """.formatted(packageName, enumName, constants, enumName, enumName, enumName));
            }
            if ("List".equals(field.type()) && "Object".equals(field.itemType())
                    && (field.itemFields() == null || field.itemFields().isEmpty())) {
                throw new IllegalArgumentException("Object List field needs item fields: " + field.name());
            }
        }
    }

    private static String enumConstant(String value) {
        String constant = value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        return constant.isBlank() ? "VALUE" : (Character.isDigit(constant.charAt(0)) ? "VALUE_" + constant : constant);
    }

    private static String listItemClassName(String ownerClass, FieldDefinition field) {
        return toPascalCase(field.name()) + "Item";
    }

    private static String innerObjectListClasses(List<FieldDefinition> fields) {
        StringBuilder source = new StringBuilder();
        Set<String> generated = new LinkedHashSet<>();
        for (FieldDefinition field : fields == null ? List.<FieldDefinition>of() : fields) {
            if (field == null || !"List".equals(field.type()) || !"Object".equals(field.itemType())
                    || field.name() == null || field.name().isBlank()) {
                continue;
            }
            String itemName = listItemClassName("", field);
            if (!generated.add(itemName)) continue;
            List<FieldDefinition> itemFields = field.itemFields() == null ? List.of() : field.itemFields();
            if (itemFields.isEmpty()) {
                throw new IllegalArgumentException("Object List field needs item fields: " + field.name());
            }
            source.append("\n    @Data\n    public static class ").append(itemName).append(" {\n")
                    .append(fieldLines(itemFields, Set.of(), itemName))
                    .append(innerObjectListClasses(itemFields))
                    .append("    }\n");
        }
        return source.toString();
    }

    private static String httpUseCaseImplContent(String bizPackage, String baseName, String httpPackage,
                                                 String httpApiClass, String author, String createDate) {
        String httpRequestClass = baseName + "HttpRequest";
        String httpResponseClass = baseName + "HttpResponse";
        String httpClientClass = httpApiClass + "Client";
        String clientVariable = Character.toLowerCase(httpClientClass.charAt(0)) + httpClientClass.substring(1);
        String methodName = Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1);
        String result = """
                package %s.usecase.impl;

                import %s.converter.%sConverter;
                import %s.dto.%sRequest;
                import %s.dto.%sResponse;
                import %s.%s.%s;
                import %s.%s.io.%s;
                import %s.%s.io.%s;
                import %s.usecase.%sUseCase;
                import lombok.RequiredArgsConstructor;
                import org.springframework.stereotype.Service;

                @Service
                @RequiredArgsConstructor
                public class %sUseCaseImpl implements %sUseCase {

                    private final %sConverter converter;
                    private final %s %s;

                    @Override
                    public %sResponse execute(%sRequest req) {
                        %s httpRequest = converter.toHttpRequest(req);
                        %s httpResponse = %s.call(httpRequest, %s.class);

                        %sResponse response = converter.toResponse(httpResponse);
                        response.setResultCode("SUCCESS");
                        response.setResultMessage("HTTP API call completed.");
                        return response;
                    }
                }
                """.formatted(
                bizPackage,
                bizPackage, baseName,
                bizPackage, baseName,
                bizPackage, baseName,
                BASE_PACKAGE, httpPackage, httpClientClass,
                BASE_PACKAGE, httpPackage, httpRequestClass,
                BASE_PACKAGE, httpPackage, httpResponseClass,
                bizPackage, baseName,
                baseName, baseName,
                baseName, httpClientClass, clientVariable,
                baseName, baseName,
                httpRequestClass, httpResponseClass, clientVariable, httpResponseClass,
                baseName);
        return result.replace("execute(", methodName + "(");
    }

    private static String httpClientContent(String httpPackage, String clientClass, String apiName) {
        return """
                package %s;

                import io.shinhanlife.dat.lib.integration.http.component.AxhubHttpComponent;
                import lombok.RequiredArgsConstructor;
                import org.springframework.stereotype.Component;

                @Component
                @RequiredArgsConstructor
                public class %s {
                    private static final String API_NAME = "%s";

                    private final AxhubHttpComponent http;

                    public <I, O> O call(I request, Class<O> responseType) {
                        return http.call(API_NAME, request, responseType);
                    }
                }
                """.formatted(httpPackage, clientClass, apiName);
    }

    private static String httpConverterContent(String bizPackage, String baseName, String httpPackage) {
        return """
                package %s.converter;

                import %s.dto.%sRequest;
                import %s.dto.%sResponse;
                import %s.io.%sHttpRequest;
                import %s.io.%sHttpResponse;
                import org.mapstruct.Mapper;
                import org.mapstruct.Mapping;
                import org.mapstruct.ReportingPolicy;

                @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
                public interface %sConverter {
                    // Field names differ? Add mappings like this before the method.
                    // @Mapping(source = "sourceField", target = "targetField")
                    %sHttpRequest toHttpRequest(%sRequest request);
                    %sResponse toResponse(%sHttpResponse httpResponse);
                }
                """.formatted(bizPackage,
                bizPackage, baseName,
                bizPackage, baseName,
                httpPackage, baseName,
                httpPackage, baseName,
                baseName, baseName, baseName, baseName, baseName);
    }

    private static String toPackageSegment(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "http_api" : normalized;
    }
    private static String mciConverterContent(String converterPackage, String bizPackage, String baseName,
                                              String mciPackage, String interfaceId, String converterClassName) {
        return """
                package %s;

                import %s.dto.%sRequest;
                import %s.dto.%sResponse;
                import %s.%s.io.%s_I;
                import %s.%s.io.%s_O;
                import org.mapstruct.Mapper;
                import org.mapstruct.Mapping;
                import org.mapstruct.ReportingPolicy;

                @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
                public interface %s {
                    // Field names differ? Add mappings like this before the method.
                    // @Mapping(source = "sourceField", target = "targetField")
                    %s_I toLegacyRequest(%sRequest request);
                    %sRequest toRequest(%s_I mciRequest);
                    %sResponse toResponse(%s_O mciRes);
                }
                """.formatted(converterPackage, bizPackage, baseName, bizPackage, baseName,
                BASE_PACKAGE, mciPackage, interfaceId, BASE_PACKAGE, mciPackage, interfaceId,
                converterClassName, interfaceId, baseName, baseName, interfaceId, baseName, interfaceId);
    }

    private static String mciTargetSystemPackage(String clientSystemCode) {
        if (clientSystemCode == null || clientSystemCode.length() != 9) {
            return null;
        }
        String prefix = clientSystemCode.substring(1, 5).toLowerCase(Locale.ROOT);
        return prefix.substring(0, 3) + "." + prefix.substring(3);
    }

    private static String legacyConverterContent(String bizPackage, String baseName) {
        return """
                package %s.converter;

                import %s.dto.%sRequest;
                import %s.dto.%sResponse;
                import %s.legacy.%sLegacyRequest;
                import %s.legacy.%sLegacyResponse;
                import org.mapstruct.Mapper;
                import org.mapstruct.Mapping;
                import org.mapstruct.ReportingPolicy;

                @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
                public interface %sConverter {
                    // Field names differ? Add mappings like this before the method.
                    // @Mapping(source = "sourceField", target = "targetField")
                    %sLegacyRequest toLegacyRequest(%sRequest request);
                    %sRequest toRequest(%sLegacyRequest legacyRequest);
                    %sResponse toResponse(%sLegacyResponse legacyResponse);
                }
                """.formatted(
                bizPackage, bizPackage, baseName, bizPackage, baseName,
                bizPackage, baseName, bizPackage, baseName,
                baseName, baseName, baseName, baseName, baseName, baseName, baseName);
    }
    private static String fieldLines(List<FieldDefinition> fields) {
        return fieldLines(fields, Set.of(), "");
    }

    private static String fieldLines(List<FieldDefinition> fields, Set<String> excludedNames) {
        return fieldLines(fields, excludedNames, "");
    }

    private static String fieldLines(List<FieldDefinition> fields, Set<String> excludedNames, String ownerClass) {
        StringBuilder source = new StringBuilder();
        Set<String> generatedNames = new LinkedHashSet<>();
        for (FieldDefinition field : fields == null ? List.<FieldDefinition>of() : fields) {
            if (field == null || field.name() == null || field.name().isBlank()) {
                continue;
            }
            String fieldName = field.name().trim();
            if (excludedNames.contains(fieldName) || !generatedNames.add(fieldName)) {
                continue;
            }
            String type = javaFieldType(field, ownerClass);
            String description = javaText(richDescription(field));
            String example = "";
            if (field.examples() != null && !field.examples().isEmpty()) {
                example = javaText(field.examples().get(0));
            }
            source.append("    @Schema(description = \"").append(description).append("\"");
            if (!example.isEmpty()) {
                source.append(", example = \"").append(example).append("\"");
            }
            if (field.required()) {
                source.append(", requiredMode = Schema.RequiredMode.REQUIRED");
            }
            source.append(")\n");
            if (field.pattern() != null && !field.pattern().isBlank()) {
                source.append("    @Pattern(regexp = \"").append(javaText(field.pattern())).append("\")\n");
            }
            source.append("    private ").append(type).append(' ').append(fieldName).append(";\n\n");
        }
        return source.toString();
    }

    private static String richDescription(FieldDefinition field) {
        String desc = field.description() == null ? "" : field.description().trim();
        if (field.pattern() != null && !field.pattern().isBlank()) {
            desc += " (형식: " + field.pattern() + ")";
        }
        return desc.trim();
    }

    private static String javaFieldType(FieldDefinition field, String ownerClass) {
        return switch (field.type() == null ? "String" : field.type()) {
            case "Enum" -> toPascalCase(field.name());
            case "List" -> "List<" + listItemJavaType(field, ownerClass) + ">";
            default -> supportedType(field.type());
        };
    }

    private static String listItemJavaType(FieldDefinition field, String ownerClass) {
        String itemType = field.itemType() == null ? "" : field.itemType();
        if ("Object".equals(itemType)) {
            return listItemClassName(ownerClass, field);
        }
        return supportedType(itemType);
    }
    private static String supportedType(String type) {
        if (type == null || type.isBlank()) return "String";
        return type;
    }


    static String wireMockMappingContent(String interfaceId, String bodyFileName) {
        return """
                {
                  "request" : {
                    "method" : "POST",
                    "urlPath" : "/%s"
                  },
                  "response" : {
                    "status" : 200,
                    "headers" : {
                      "Content-Type" : "application/json;charset=UTF-8"
                    },
                    "bodyFileName" : "%s"
                  }
                }
                """.formatted(jsonEscape(interfaceId), jsonEscape(bodyFileName));
    }
    private static String mockResponseContent(List<FieldDefinition> outputFields) {
        StringBuilder json = new StringBuilder("{\n");
        List<FieldDefinition> fields = outputFields == null ? List.of() : outputFields;
        boolean first = true;
        for (FieldDefinition field : fields) {
            if (field.name() == null || field.name().isBlank()) {
                continue;
            }
            if (!first) {
                json.append(",\n");
            }
            json.append("  \"").append(jsonEscape(field.name())).append("\" : ")
                    .append(mockValue(field));
            first = false;
        }
        return json.append("\n}\n").toString();
    }

    private static String mockValue(FieldDefinition field) {
        if ("List".equals(field.type())) {
            if ("Object".equals(field.itemType())) {
                StringBuilder object = new StringBuilder("{");
                boolean first = true;
                for (FieldDefinition itemField : field.itemFields() == null ? List.<FieldDefinition>of() : field.itemFields()) {
                    if (!first) object.append(", ");
                    object.append("\"").append(jsonEscape(itemField.name())).append("\" : ").append(mockValue(itemField));
                    first = false;
                }
                return "[" + object + "]";
            }
            FieldDefinition item = new FieldDefinition("item", field.itemType(), "", field.examples(), field.pattern(), false);
            return "[" + mockValue(item) + "]";
        }

        String exampleStr = (field.examples() != null && !field.examples().isEmpty()) ? field.examples().get(0) : null;
        if (exampleStr == null || exampleStr.isBlank()) {
            return "null";
        }
        if ("Enum".equals(field.type())) {
            return "\"" + jsonEscape(exampleStr) + "\"";
        }
        return switch (supportedType(field.type())) {
            case "Integer", "Long", "Double", "BigDecimal" -> exampleStr;
            case "Boolean" -> Boolean.parseBoolean(exampleStr) ? "true" : "false";
            default -> "\"" + jsonEscape(exampleStr) + "\"";
        };
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String useCaseTestContent(String bizPackage, String baseName) {
        return """
                package %s.usecase;

                import static org.junit.jupiter.api.Assertions.assertNotNull;

                import %s.dto.%sRequest;
                import %s.dto.%sResponse;
                import org.junit.jupiter.api.Test;

                class %sUseCaseTest {

                    @Test
                    void createsToolRequestAndResponseDtos() {
                        assertNotNull(new %sRequest());
                        assertNotNull(new %sResponse());
                    }
                }
                """.formatted(bizPackage, bizPackage, baseName, bizPackage, baseName,
                baseName, baseName, baseName);
    }
    private static String toToolName(String moduleName, String group, String baseName) {
        String normalizedName = baseName.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        String[] words = normalizedName.split("\\s+");
        String service = words[0];
        String action = words.length == 1 ? "execute" : words[words.length - 1];
        return "%s_%s_%s".formatted(
                group.toLowerCase(Locale.ROOT),
                service,
                action);
    }

    private static String abbreviatedMciSourceBaseName(String baseName) {
        String[] words = baseName.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");
        if (words.length < 3) {
            return baseName;
        }
        return abbreviatedWord(words[0], 4) + abbreviatedWord(words[1], 6);
    }

    private static String abbreviatedWord(String word, int maximumLength) {
        int length = Math.min(word.length(), maximumLength);
        return toPascalCase(word.substring(0, length).toLowerCase(Locale.ROOT));
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
