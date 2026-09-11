package io.shinhanlife.dat.lib.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PodScaffolder {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=========================================");
        System.out.println("   MCP Tool Pod Scaffolder (Java CLI)   ");
        System.out.println("=========================================\n");

        String rawModuleName = getOrAsk(args, 0, scanner, "1. 생성할 모듈(Pod) 이름 (예: payment 또는 dat-was-payment): ");
        String moduleName = rawModuleName.startsWith("dat-was-") ? rawModuleName : "dat-was-" + rawModuleName;
        String portStr = getOrAsk(args, 1, scanner, "2. 사용할 포트 번호 (예: 8085): ");
        String shortName = moduleName.replace("dat-was-", "").replace("-", "");

        String defaultAuthor = System.getProperty("user.name");
        String defaultDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

        String author = getOrAsk(args, 2, scanner, "3. 작성자 (엔터 입력 시 '" + defaultAuthor + "'): ");
        if (author.trim().isEmpty()) author = defaultAuthor;
        String createDate = getOrAsk(args, 3, scanner, "4. 작성일 (엔터 입력 시 '" + defaultDate + "'): ");
        if (createDate.trim().isEmpty()) createDate = defaultDate;

        String result = scaffoldPod(moduleName, portStr, shortName, author, createDate);
        System.out.println(result);
    }

    private static String getOrAsk(String[] args, int index, Scanner scanner, String prompt) {
        if (args.length > index) {
            return args[index];
        }
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static String scaffoldPod(String moduleName, String portStr, String shortName, String author, String createDate) throws IOException {
        String envSourceDir = System.getProperty("AXHUB_SOURCE_DIR");
        if (envSourceDir == null || envSourceDir.isBlank()) {
            envSourceDir = System.getenv("AXHUB_SOURCE_DIR");
        }
        Path rootDir = envSourceDir != null ? Paths.get(envSourceDir) : Paths.get(".");
        return scaffoldPod(rootDir, moduleName, portStr, shortName, author, createDate, null);
    }

    public static String scaffoldPod(String moduleName, String portStr, String shortName, String author,
                                     String createDate, String toolServiceManifest) throws IOException {
        return scaffoldPod(moduleName, portStr, shortName, author, createDate, toolServiceManifest, null);
    }

    public static String scaffoldPod(String moduleName, String portStr, String shortName, String author,
                                     String createDate, String toolServiceManifest, List<String> targetModules) throws IOException {
        String envSourceDir = System.getProperty("AXHUB_SOURCE_DIR");
        if (envSourceDir == null || envSourceDir.isBlank()) envSourceDir = System.getenv("AXHUB_SOURCE_DIR");
        Path rootDir = envSourceDir != null ? Paths.get(envSourceDir) : Paths.get(".");
        return scaffoldPod(rootDir, moduleName, portStr, shortName, author, createDate, toolServiceManifest, targetModules);
    }

    static String scaffoldPod(Path rootDir, String moduleName, String portStr, String shortName,
                              String author, String createDate) throws IOException {
        return scaffoldPod(rootDir, moduleName, portStr, shortName, author, createDate, null);
    }

    static String scaffoldPod(Path rootDir, String moduleName, String portStr, String shortName,
                              String author, String createDate, String toolServiceManifest) throws IOException {
        return scaffoldPod(rootDir, moduleName, portStr, shortName, author, createDate, toolServiceManifest, null);
    }

    static String scaffoldPod(Path rootDir, String moduleName, String portStr, String shortName,
                              String author, String createDate, String toolServiceManifest, List<String> targetModules) throws IOException {
        Path modulePath = rootDir.resolve(Paths.get(moduleName));
        if (Files.exists(modulePath)) {
            return "[오류] 이미 존재하는 모듈입니다: " + moduleName;
        }

        StringBuilder log = new StringBuilder();
        log.append("[1/6] 모듈 디렉터리 생성 중...\n");
        Files.createDirectories(modulePath);

        log.append("[2/6] build.gradle 생성 중...\n");
        String buildGradle = """
            plugins {
                // Spring Boot 3.5.11 version is managed by the root build.gradle.
                id 'org.springframework.boot'
            }

            dependencies {
                // Shared MCP, Glow integration, validation, logging, Lombok and MapStruct configuration.
                implementation project(':dat-was-lib')
            }
            """;
        writeUtf8(modulePath.resolve("build.gradle"), buildGradle);

        log.append("[3/6] Dockerfile 생성 중...\n");
        String dockerfile = """
            FROM eclipse-temurin:21-jre-alpine
            WORKDIR /app
            RUN apk add --no-cache tzdata
            ENV TZ=Asia/Seoul
            COPY %s/build/libs/*-SNAPSHOT.jar app.jar
            EXPOSE %s
            ENTRYPOINT ["java", "-jar", "app.jar"]
            """.formatted(moduleName, portStr);
        writeUtf8(modulePath.resolve("Dockerfile"), dockerfile);

        log.append("[4/6] Application 클래스 및 설정 파일 생성 중...\n");
        Path srcPath = modulePath.resolve("src/main/java/io/shinhanlife/dat/mcc/" + shortName);
        Files.createDirectories(srcPath);

        String appClass = """
            package io.shinhanlife.dat.mcc.%s;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
            import org.springframework.cache.annotation.EnableCaching;
            
            /**
             * @package io.shinhanlife.dat.mcc.%s
             * @className DatWas%sApplication
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
            @SpringBootApplication(scanBasePackages = {"io.shinhanlife.dat.mcc", "io.shinhanlife.dat.lib"})
            @ConfigurationPropertiesScan(basePackages = {"io.shinhanlife.dat.mcc", "io.shinhanlife.dat.lib"})
            @EnableCaching
            public class DatWas%sApplication {
                public static void main(String[] args) {
                    SpringApplication.run(DatWas%sApplication.class, args);
                }
            }
            """.formatted(shortName, shortName, capitalize(shortName), author, createDate, createDate, author, capitalize(shortName), capitalize(shortName));
        writeUtf8(srcPath.resolve("DatWas" + capitalize(shortName) + "Application.java"), appClass);

        Path resPath = modulePath.resolve("src/main/resources");
        Files.createDirectories(resPath);
        String applicationYml = """
            server:
              port: %s
            spring:
              application:
                name: %s
              config:
                import: optional:classpath:tool-service-manifest.yml
              profiles:
                active: local
            logging:
              level:
                org.apache.kafka: ERROR
            mcp:
              namespace: ""
              manifest:
                bundle-id: %s
                name-prefix: ""
              security:
                api-key: ${TOOL_SERVER_API_KEY:tool-server-key}
                tenant-domains:
                  TESTER-DEV: ALL
            """.formatted(portStr, moduleName, moduleName.replace("dap-", ""));
        writeUtf8(resPath.resolve("application.yml"), applicationYml);
        String manifest = normalizeToolServiceManifest(
                toolServiceManifest == null || toolServiceManifest.isBlank()
                        ? defaultToolServiceManifest(moduleName)
                        : toolServiceManifest,
                rootDir, moduleName, shortName, targetModules);
        writeUtf8(resPath.resolve("tool-service-manifest.yml"), manifest);

        String applicationLocalYml = """
            # Local 환경 전용 설정 (H2 메모리 DB 등)
            spring:
              config:
                activate:
                  on-profile: local
                import:
                  - classpath:application-core-local.yml
                  - classpath:glow/application-glow.yml
                  - classpath:glow/application-glow-local.yml
              datasource:
                username: sa
                password: password
              h2:
                console:
                  enabled: true

            eims:
              http:
                url: http://localhost:${server.port}/api/gateway
              tcp:
                host: 127.0.0.1
                port: 8090
                timeout: 5000
              jsp:
                form:
                  url: http://localhost:${server.port}/mock/jsp-form
                json:
                  url: http://localhost:${server.port}/mock/jsp-json
              mci:
                url: http://localhost:${server.port}/api/mock/esb/api
              mcistring:
                url: http://localhost:${server.port}/api/mock/esb/string

            mcp:
              security:
                tenant-domains:
                  mcp-client-1: CUSTOMER,COMMON
                  mcp-client-2: ALL

            axhub:
              gateway:
                url: http://localhost:8081
              tool:
                url: ${AXHUB_TOOL_URL:http://localhost:${server.port}}
            """;
        writeUtf8(resPath.resolve("application-local.yml"), applicationLocalYml);

        String applicationDevYml = """
            # OCI 클라우드 환경 전용 설정
            server:
              port: ${PORT:%s}

            spring:
              config:
                activate:
                  on-profile: dev
                import:
                  - classpath:application-core-dev.yml
                  - classpath:glow/application-glow.yml
                  - classpath:glow/application-glow-dev.yml

            axhub:
              gateway:
                url: https://axhubmcp.devjun.net
              tool:
                url: http://144.24.70.100:%s

            eims:
              http:
                url: http://localhost:${server.port}/api/gateway
              tcp:
                host: 127.0.0.1
                port: 8090
                timeout: 5000
              jsp:
                form:
                  url: http://localhost:${server.port}/mock/jsp-form
                json:
                  url: http://localhost:${server.port}/mock/jsp-json
              mci:
                url: http://localhost:${server.port}/api/mock/esb/api
              mcistring:
                url: http://localhost:${server.port}/api/mock/esb/string

            shinhan:
              integration:
                envrTypeCd: D
                eai:
                  url: http://10.176.32.181
                internalMci:
                  url: http://10.176.32.173
                bancaMci:
                  url: http://10.176.32.117
                externalMci:
                  url: http://10.176.32.176
            """.formatted(portStr, portStr);
        writeUtf8(resPath.resolve("application-dev.yml"), applicationDevYml);

        String applicationTestYml = """
            server:
              port: ${PORT:%s}

            spring:
              config:
                activate:
                  on-profile: test
                import:
                  - classpath:application-core-test.yml
                  - classpath:glow/application-glow.yml
                  - classpath:glow/application-glow-test.yml

            axhub:
              gateway:
                url: ${AXHUB_GATEWAY_URL}
              tool:
                url: ${AXHUB_TOOL_URL}
            """.formatted(portStr);
        writeUtf8(resPath.resolve("application-test.yml"), applicationTestYml);

        String applicationProdYml = """
            server:
              port: ${PORT:%s}

            spring:
              config:
                activate:
                  on-profile: prod
                import:
                  - classpath:application-core-prod.yml
                  - classpath:glow/application-glow.yml
                  - classpath:glow/application-glow-prod.yml

            axhub:
              gateway:
                url: ${AXHUB_GATEWAY_URL}
              tool:
                url: ${AXHUB_TOOL_URL}
            """.formatted(portStr);
        writeUtf8(resPath.resolve("application-prod.yml"), applicationProdYml);

        // 환경별 파일은 Core/Glow 프로필 import만 유지한다. 업무 연동·인프라 주소는 각 환경의 Core/Glow 설정에서 관리한다.
        for (String profile : List.of("local", "dev", "test", "prod")) {
            writeUtf8(resPath.resolve("application-" + profile + ".yml"), applicationProfileYml(profile, portStr));
        }

        String logbackXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <configuration>
                <property name="LOG_PATTERN" value="%%d{yyyy-MM-dd HH:mm:ss.SSS} [%%thread] [%%X{traceId}] %%-5level %%logger{36} - %%msg%%n" />
                <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
                    <encoder>
                        <pattern>${LOG_PATTERN}</pattern>
                    </encoder>
                </appender>
                <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
                    <file>logs/%s.log</file>
                    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                        <fileNamePattern>logs/%s-%%d{yyyy-MM-dd}.log</fileNamePattern>
                        <maxHistory>30</maxHistory>
                    </rollingPolicy>
                    <encoder>
                        <pattern>${LOG_PATTERN}</pattern>
                    </encoder>
                </appender>
                <root level="INFO">
                    <appender-ref ref="CONSOLE" />
                    <appender-ref ref="FILE" />
                </root>
                <logger name="io.shinhanlife" level="DEBUG" />
            </configuration>
            """.formatted(moduleName, moduleName);
        writeUtf8(resPath.resolve("logback-spring.xml"), logbackXml);

        log.append("[5/6] settings.gradle 에 모듈 등록 중...\n");
        Path settingsPath = rootDir.resolve(Paths.get("settings.gradle"));
        if (Files.exists(settingsPath)) {
            String settings = Files.readString(settingsPath, StandardCharsets.UTF_8);
            if (!settings.contains("include '" + moduleName + "'")) {
                Files.writeString(settingsPath, System.lineSeparator() + "include '" + moduleName + "'" + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            }
        }

        log.append("[6/6] docker-compose.yml 에 서비스 추가 중...\n");
        Path dockerComposePath = rootDir.resolve(Paths.get("docker-compose.yml"));
        if (Files.exists(dockerComposePath)) {
            String compose = Files.readString(dockerComposePath, StandardCharsets.UTF_8);
            String serviceName = moduleName.replaceFirst("^dat-was-", "was-");
            if (!compose.contains("  " + serviceName + ":")) {
                String newService = """
                      %s:
                        build: 
                          context: .
                          dockerfile: %s/Dockerfile
                        ports:
                          - "%s:%s"
                        environment:
                          - TZ=Asia/Seoul
                          - TOOL_SERVER_API_KEY=${TOOL_SERVER_API_KEY:-tool-server-key}
                          - AXHUB_GATEWAY_URL=http://gateway:8081
                          - AXHUB_TOOL_URL=http://%s:%s
                          - GLOW_COMMUNICATION_MCI_HOST=http://mci-mock
                          - GLOW_COMMUNICATION_MCI_PORT=8080
                          - GLOW_COMMUNICATION_EXTMCI_HOST=http://mci-mock
                          - GLOW_COMMUNICATION_EXTMCI_PORT=8080
                          - GLOW_COMMUNICATION_EAI_HOST=http://mci-mock
                          - GLOW_COMMUNICATION_EAI_PORT=8080
                          - SPRING_PROFILES_ACTIVE=${ACTIVE_PROFILE:-local}
                    """.formatted(serviceName, moduleName, portStr, portStr, serviceName, portStr);
                Files.writeString(dockerComposePath, System.lineSeparator() + newService, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            }
        }

        List<String> updatedManifests = updateReciprocalConfusableServers(rootDir, moduleName, manifest);
        if (!updatedManifests.isEmpty()) {
            log.append("[추가] 기존 Pod confusable-servers 갱신: ")
                    .append(String.join(", ", updatedManifests)).append("\n");
        }

        log.append("\n=========================================\n");
        log.append("  Pod Scaffolding Complete! \n");
        log.append("=========================================\n");
        log.append("1. [새로운 모듈] ").append(moduleName).append(" 폴더가 생성되었습니다.\n");
        log.append("2. [ToolScaffolder]를 사용해 이 모듈 안에 툴을 추가하세요.\n");
        log.append("3. 실행 전 Gradle 동기화(Sync)를 한 번 진행해 주세요.\n");
        return log.toString();
    }

    private static void writeUtf8(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static String defaultToolServiceManifest(String moduleName) {
        String key = moduleName.replace("dat-was-", "");
        return """
            mcp:
              manifest:
                routing-functions:
                  - name: route_to_%s
                    description-serialization: %s 업무 서버로 요청을 라우팅합니다. %s 관련 업무를 처리합니다. 요청의 주요 업무 영역을 기준으로 서버를 선택합니다.
                    server-id: %s
                    category-key: %s
                    product-boundary: insurance
                    business-domain: %s 업무
                    business-outcome: %s 관련 업무를 처리합니다.
                    primary-entities: []
                    capabilities: []
                    select-if: %s 관련 요청인 경우
                    reject-if: 다른 업무 영역이 주된 요청인 경우
                    confusable-servers: []
                    decision-policy: 요청의 주요 업무 영역을 기준으로 서버를 선택합니다.
            """.formatted(moduleName, key, key, moduleName, key, key, key, key);
    }

    private static String applicationProfileYml(String profile, String port) {
        if ("local".equals(profile)) {
            return """
                    spring:
                      config:
                        activate:
                          on-profile: local
                        import:
                          - classpath:glow/application-glow.yml
                          - classpath:glow/application-glow-local.yml
                    """;
        }
        return """
            server:
              port: ${PORT:%s}

            spring:
              config:
                activate:
                  on-profile: %s
                import:
                  - classpath:application-core-%s.yml
                  - classpath:glow/application-glow.yml
                  - classpath:glow/application-glow-%s.yml
            """.formatted(port, profile, profile, profile);
    }

    @SuppressWarnings("unchecked")
    public static String normalizeToolServiceManifest(String source, String moduleName, List<String> targetModules)
            throws IOException {
        return normalizeToolServiceManifest(source, null, moduleName,
                moduleName.replaceFirst("^dat-was-", ""), targetModules);
    }

    private static String normalizeToolServiceManifest(String source, Path rootDir, String moduleName, String categoryKey,
                                                        List<String> targetModules)
            throws IOException {
        Map<String, Object> root = parseManifestYaml(source);
        Map<String, Object> manifest = manifestNode(root);
        if (manifest == null) {
            throw new IOException("tool-service-manifest.yml의 mcp.manifest.routing-functions 형식이 올바르지 않습니다.");
        }

        Object routingFunctions = manifest.get("routing-functions");
        Map<String, Object> routingFunction = routingFunction(routingFunctions);
        if (routingFunction == null) {
            throw new IOException("tool-service-manifest.yml의 routing-functions에 라우팅 함수가 없습니다.");
        }
        routingFunction.put("name", "route_to_" + moduleName);
        routingFunction.put("server-id", moduleName);
        routingFunction.put("category-key", categoryKey);
        routingFunction.put("confusable-servers", allowedTargetModules(rootDir, moduleName, targetModules));
        manifest.put("routing-functions", List.of(routingFunction));
        return YAML_MAPPER.writeValueAsString(root);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> manifestNode(Map<String, Object> root) {
        if (root.get("mcp") instanceof Map<?, ?> mcp
                && mcp.get("manifest") instanceof Map<?, ?> manifest) {
            return (Map<String, Object>) manifest;
        }

        Object routingFunctions = root.remove("mcp.manifest.routing-functions");
        if (routingFunctions == null) {
            return null;
        }
        Map<String, Object> manifest = new java.util.LinkedHashMap<>();
        manifest.put("routing-functions", routingFunctions);
        Map<String, Object> mcp = new java.util.LinkedHashMap<>();
        mcp.put("manifest", manifest);
        root.put("mcp", mcp);
        return manifest;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> routingFunction(Object routingFunctions) {
        if (routingFunctions instanceof List<?> functions
                && !functions.isEmpty()
                && functions.getFirst() instanceof Map<?, ?> function) {
            return new java.util.LinkedHashMap<>((Map<String, Object>) function);
        }
        if (routingFunctions instanceof Map<?, ?> functionMap) {
            if (functionMap.containsKey("name")) {
                return new java.util.LinkedHashMap<>((Map<String, Object>) functionMap);
            }
            for (Object value : functionMap.values()) {
                if (value instanceof Map<?, ?> function) {
                    return new java.util.LinkedHashMap<>((Map<String, Object>) function);
                }
            }
        }
        return null;
    }

    private static List<String> existingPodModules(Path rootDir, String moduleName) throws IOException {
        try (Stream<Path> paths = Files.list(rootDir)) {
            return paths.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.matches("^dat-was-[a-z0-9-]+$"))
                    .filter(name -> !name.equals("dat-was-lib"))
                    .filter(name -> !name.equals(moduleName))
                    .sorted()
                    .toList();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseManifestYaml(String source) throws IOException {
        try {
            return YAML_MAPPER.readValue(source, Map.class);
        } catch (IOException original) {
            String normalized = quoteUnquotedManifestScalars(source);
            if (normalized.equals(source)) {
                throw original;
            }
            return YAML_MAPPER.readValue(normalized, Map.class);
        }
    }

    private static String quoteUnquotedManifestScalars(String source) {
        Set<String> stringKeys = Set.of(
                "name", "server-id", "category-key", "product-boundary", "business-domain",
                "business-outcome", "select-if", "reject-if", "decision-policy", "description-serialization");
        Pattern property = Pattern.compile("^(\\s*)([a-z-]+):(\\s*)(.*)$");
        Pattern listItem = Pattern.compile("^(\\s*-\\s+)(.*)$");
        StringBuilder normalized = new StringBuilder();
        for (String line : source.split("\\r?\\n", -1)) {
            Matcher propertyMatcher = property.matcher(line);
            if (propertyMatcher.matches() && stringKeys.contains(propertyMatcher.group(2))) {
                String value = propertyMatcher.group(4).trim();
                if (!value.isEmpty() && !isQuoted(value)) {
                    line = propertyMatcher.group(1) + propertyMatcher.group(2) + ": " + yamlQuoted(value);
                }
            } else {
                Matcher listItemMatcher = listItem.matcher(line);
                if (listItemMatcher.matches()) {
                    String value = listItemMatcher.group(2).trim();
                    if (value.contains(":") && !isQuoted(value) && !value.matches("^[a-z-]+:.*$")) {
                        line = listItemMatcher.group(1) + yamlQuoted(value);
                    }
                }
            }
            normalized.append(line).append(System.lineSeparator());
        }
        return normalized.toString();
    }

    private static boolean isQuoted(String value) {
        return (value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"));
    }

    private static String yamlQuoted(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static List<String> allowedTargetModules(Path rootDir, String moduleName, List<String> targetModules)
            throws IOException {
        List<String> candidates = targetModules == null || targetModules.isEmpty()
                ? existingPodModules(rootDir, moduleName)
                : targetModules;
        LinkedHashSet<String> allowed = new LinkedHashSet<>();
        for (String candidate : candidates) {
            String normalized = candidate == null ? "" : candidate.trim();
            if (normalized.matches("^dat-was-[a-z0-9-]+$") && !normalized.equals(moduleName)) {
                allowed.add(normalized);
            }
        }
        return List.copyOf(allowed);
    }

    private static List<String> updateReciprocalConfusableServers(Path rootDir, String moduleName,
                                                                   String newManifest) throws IOException {
        List<String> updated = new ArrayList<>();
        for (String target : extractConfusableServers(newManifest)) {
            if (target.equals(moduleName) || !target.matches("^dat-was-[a-z0-9-]+$")) continue;
            Path path = rootDir.resolve(target).resolve("src/main/resources/tool-service-manifest.yml");
            if (!Files.isRegularFile(path)) continue;
            String before = Files.readString(path, StandardCharsets.UTF_8);
            String after = addConfusableServer(before, moduleName);
            if (!before.equals(after)) {
                writeUtf8(path, after);
                updated.add(rootDir.relativize(path).toString().replace('\\', '/'));
            }
        }
        return updated;
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractConfusableServers(String manifest) throws IOException {
        Map<String, Object> root = YAML_MAPPER.readValue(manifest, Map.class);
        Object mcpValue = root.get("mcp");
        if (!(mcpValue instanceof Map<?, ?> mcp)) return List.of();
        Object manifestValue = mcp.get("manifest");
        if (!(manifestValue instanceof Map<?, ?> manifestMap)) return List.of();
        Object functionsValue = manifestMap.get("routing-functions");
        if (!(functionsValue instanceof List<?> functions) || functions.isEmpty()
                || !(functions.getFirst() instanceof Map<?, ?> function)) return List.of();
        Object serversValue = function.get("confusable-servers");
        if (!(serversValue instanceof Collection<?> servers)) return List.of();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (Object value : servers) {
            String normalized = value == null ? "" : value.toString().trim();
            if (!normalized.isBlank()) values.add(normalized);
        }
        return List.copyOf(values);
    }

    private static String addConfusableServer(String manifest, String moduleName) {
        Pattern pattern = Pattern.compile("(?m)^(\\s*)confusable-servers:\\s*\\[([^]]*)]\\s*$");
        Matcher matcher = pattern.matcher(manifest);
        if (matcher.find()) {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (String value : matcher.group(2).split(",")) {
                String normalized = value.trim();
                if (!normalized.isBlank()) values.add(normalized);
            }
            if (!values.add(moduleName)) return manifest;
            String replacement = matcher.group(1) + "confusable-servers: [" + String.join(", ", values) + "]";
            return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
        }
        Matcher category = Pattern.compile("(?m)^(\\s*)category-key:[^\\r\\n]*$").matcher(manifest);
        if (!category.find()) return manifest;
        String replacement = category.group() + System.lineSeparator() + category.group(1)
                + "confusable-servers: [" + moduleName + "]";
        return category.replaceFirst(Matcher.quoteReplacement(replacement));
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
