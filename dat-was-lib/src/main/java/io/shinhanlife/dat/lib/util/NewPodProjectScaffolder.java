package io.shinhanlife.dat.lib.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** Creates a standalone Tool Pod repository without changing the legacy PodScaffolder flow. */
public final class NewPodProjectScaffolder {

    private static final Pattern MODULE_NAME = Pattern.compile("^dat-was-[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private NewPodProjectScaffolder() {
    }

    public static String scaffold(Path workspaceRoot, String moduleName, int port, String author, String createdDate)
            throws IOException {
        return scaffold(workspaceRoot, moduleName, port, author, createdDate, null, List.of());
    }

    public static String scaffold(Path workspaceRoot, String moduleName, int port, String author, String createdDate,
                                  String toolServiceManifest, List<String> targetModules)
            throws IOException {
        validateModuleName(moduleName);
        validatePort(port);
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("Workspace directory does not exist: null");
        }
        Path selectedPath = workspaceRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(selectedPath)) {
            throw new IllegalArgumentException("Workspace directory does not exist: " + workspaceRoot);
        }
        if (isPodProjectRoot(selectedPath)) {
            return scaffoldModuleProject(selectedPath, moduleName, port, author, createdDate,
                    toolServiceManifest, targetModules);
        }
        Path normalizedWorkspace = selectedPath;
        Path target = normalizedWorkspace.resolve(moduleName).normalize();
        if (!target.getParent().equals(normalizedWorkspace)) {
            throw new IllegalArgumentException("Module path must be directly below the workspace");
        }
        if (Files.exists(target)) {
            throw new IllegalStateException("Project already exists: " + moduleName);
        }
        Path libraryProject = normalizedWorkspace.resolve("dat-lib-datmt");
        if (!Files.isDirectory(libraryProject)) {
            throw new IllegalArgumentException("dat-lib-datmt project does not exist below the workspace: " + libraryProject);
        }

        Path temporary = Files.createTempDirectory(normalizedWorkspace, ".new-pod-");
        try {
            writeProject(temporary, moduleName, port, blankToDefault(author, System.getProperty("user.name")),
                    blankToDefault(createdDate, "unknown"), libraryProject, toolServiceManifest, targetModules);
            moveIntoPlace(temporary, target);
            updateSelectedSiblingManifests(normalizedWorkspace, moduleName, targetModules);
            return "독립 Pod 프로젝트 생성 완료: " + target;
        } catch (IOException | RuntimeException error) {
            deleteRecursively(temporary);
            throw error;
        }
    }

    private static String scaffoldModuleProject(Path projectRoot, String moduleName, int port, String author,
                                                String createdDate, String toolServiceManifest,
                                                List<String> targetModules) throws IOException {
        Path workspace = projectRoot.getParent();
        Path libraryProject = workspace.resolve("dat-lib-datmt");
        if (!Files.isDirectory(libraryProject)) {
            throw new IllegalArgumentException("dat-lib-datmt project does not exist beside the Pod project: "
                    + libraryProject);
        }
        ensureEmptyProjectRoot(projectRoot);
        Path temporary = Files.createTempDirectory(workspace, ".new-pod-");
        try {
            writeProject(temporary, moduleName, port, blankToDefault(author, System.getProperty("user.name")),
                    blankToDefault(createdDate, "unknown"), libraryProject, toolServiceManifest, targetModules);
            convertToMultiModuleProject(temporary, projectRoot.getFileName().toString(), moduleName, port,
                    workspace.resolve("dat-was-datcu"));
            moveContentsInto(projectRoot, temporary);
            updateSelectedSiblingManifests(projectRoot, moduleName, targetModules);
            return "Pod 프로젝트 생성 완료: " + projectRoot.resolve(moduleName);
        } catch (IOException | RuntimeException error) {
            deleteRecursively(temporary);
            throw error;
        }
    }

    /** Returns the selected Pod project itself for module discovery. */
    public static Path resolveWorkspaceRoot(Path workspaceOrReferencePod) {
        Path normalizedPath = workspaceOrReferencePod.toAbsolutePath().normalize();
        return normalizedPath;
    }

    private static boolean isPodProjectRoot(Path path) {
        Path parent = path.getParent();
        Path fileName = path.getFileName();
        return parent != null && fileName != null
                && MODULE_NAME.matcher(fileName.toString()).matches()
                && Files.isDirectory(parent.resolve("dat-lib-datmt"));
    }

    private static void ensureEmptyProjectRoot(Path projectRoot) throws IOException {
        try (var files = Files.list(projectRoot)) {
            if (files.findAny().isPresent()) {
                throw new IllegalStateException("Pod project directory must be empty: " + projectRoot);
            }
        }
    }

    private static void moveContentsInto(Path projectRoot, Path temporary) throws IOException {
        try (var files = Files.list(temporary)) {
            for (Path source : files.toList()) {
                Files.move(source, projectRoot.resolve(source.getFileName()), StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException ignored) {
            try (var files = Files.list(temporary)) {
                for (Path source : files.toList()) {
                    Files.move(source, projectRoot.resolve(source.getFileName()));
                }
            }
        }
        Files.deleteIfExists(temporary);
    }

    private static void convertToMultiModuleProject(Path projectRoot, String rootProjectName, String moduleName, int port,
                                                    Path customerPodTemplate) throws IOException {
        Path moduleRoot = projectRoot.resolve(moduleName);
        Files.createDirectories(moduleRoot);
        Files.move(projectRoot.resolve("build.gradle"), moduleRoot.resolve("build.gradle"));
        Files.move(projectRoot.resolve("src"), moduleRoot.resolve("src"));
        Files.move(projectRoot.resolve("Dockerfile"), moduleRoot.resolve("Dockerfile"));
        copyCustomerGradleFormat(customerPodTemplate, projectRoot, moduleRoot, rootProjectName, moduleName);
        write(projectRoot.resolve("docker-compose.yml"), """
                services:
                  %s:
                    build:
                      context: ./%s
                    ports:
                      - "${HOST_PORT:%d}:${PORT:%d}"
                    environment:
                      - PORT=${PORT:%d}
                      - AXHUB_TOOL_URL=http://%s:${PORT:%d}
                      - SPRING_PROFILES_ACTIVE=${ACTIVE_PROFILE:-local}
                """.formatted("was-" + moduleName.substring("dat-was-".length()), moduleName,
                port, port, port, "was-" + moduleName.substring("dat-was-".length()), port));
        write(projectRoot.resolve("README.md"), """
                # %s

                Pod Module New로 생성된 DATMT Tool Pod 프로젝트입니다.

                - 서비스 모듈: `%s`
                - 공통 라이브러리: `io.shinhanlife:dat-lib-datmt:0.0.1-SNAPSHOT`

                ```powershell
                .\\gradlew.bat :%s:compileJava
                ```
                """.formatted(projectRoot.getFileName(), moduleName, moduleName));
    }

    private static void copyCustomerGradleFormat(Path templateRoot, Path projectRoot, Path moduleRoot,
                                                 String rootProjectName, String moduleName) throws IOException {
        Path templateSettings = templateRoot.resolve("settings.gradle");
        Path templateRootBuild = templateRoot.resolve("build.gradle");
        Path templateModuleBuild = templateRoot.resolve("dat-was-cus/build.gradle");
        if (!Files.isRegularFile(templateSettings) || !Files.isRegularFile(templateRootBuild)
                || !Files.isRegularFile(templateModuleBuild)) {
            throw new IllegalArgumentException("dat-was-datcu Gradle template files are missing: " + templateRoot);
        }
        String settings = Files.readString(templateSettings, StandardCharsets.UTF_8)
                .replaceFirst("(?m)^rootProject\\.name\\s*=\\s*'[^']*'",
                        "rootProject.name = '" + rootProjectName + "'")
                .replace("dat-was-cus", moduleName);
        write(projectRoot.resolve("settings.gradle"), settings);
        Files.copy(templateRootBuild, projectRoot.resolve("build.gradle"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(templateModuleBuild, moduleRoot.resolve("build.gradle"), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void validateModuleName(String moduleName) {
        if (moduleName == null || !MODULE_NAME.matcher(moduleName).matches() || "dat-was-lib".equals(moduleName)) {
            throw new IllegalArgumentException("Module name must match dat-was-<lowercase-name>: " + moduleName);
        }
    }

    public static void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535: " + port);
        }
    }

    private static void writeProject(Path root, String moduleName, int port, String author, String createdDate,
                                     Path libraryProject, String toolServiceManifest, List<String> targetModules)
            throws IOException {
        String shortName = moduleName.substring("dat-was-".length());
        String packageName = shortName.replace("-", "");
        String className = "DatWas" + pascalCase(shortName) + "Application";
        String serviceName = "was-" + shortName;
        write(root.resolve("settings.gradle"), """
                rootProject.name = '%s'
                """.formatted(moduleName));
        write(root.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'org.springframework.boot' version '3.5.11'
                    id 'io.spring.dependency-management' version '1.1.7'
                }

                group = 'io.shinhanlife'
                version = '0.0.1-SNAPSHOT'

                java {
                    toolchain { languageVersion = JavaLanguageVersion.of(21) }
                }

                repositories {
                    // dat-lib-datmt에서 :dat-was-lib:publishToMavenLocal 실행 후
                    // Maven Local의 io.shinhanlife:dat-lib-datmt JAR/POM을 사용합니다.
                    // 신한라이프 이관 시 mavenLocal() 대신 사내 Nexus repository를 추가합니다.
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    // includeBuild 없이 Maven 좌표로만 공통 MCP Server와 연동 기능을 사용합니다.
                    implementation 'io.shinhanlife:dat-lib-datmt:0.0.1-SNAPSHOT'
                }

                tasks.withType(Test).configureEach { useJUnitPlatform() }
                """);
        copyGradleWrapper(libraryProject, root);
        write(root.resolve("src/main/java/io/shinhanlife/dat/mcc/" + packageName + "/" + className + ".java"), """
                package io.shinhanlife.dat.mcc.%s;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;
                import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

                /** Generated by Pod Module New for %s on %s. */
                @SpringBootApplication(scanBasePackages = {"io.shinhanlife.dat.mcc", "io.shinhanlife.dat.lib"})
                @ConfigurationPropertiesScan(basePackages = {"io.shinhanlife.dat.mcc", "io.shinhanlife.dat.lib"})
                public class %s {
                    public static void main(String[] args) {
                        SpringApplication.run(%s.class, args);
                    }
                }
                """.formatted(packageName, author, createdDate, className, className));
        write(root.resolve("src/main/resources/application.yml"), """
                server:
                  port: ${PORT:%d}
                spring:
                  application:
                    name: %s
                  config:
                    import: optional:classpath:tool-service-manifest.yml
                  profiles:
                    active: local
                mcp:
                  namespace: %s
                  manifest:
                    bundle-id: %s
                    name-prefix: ""
                  security:
                    api-key: ${TOOL_SERVER_API_KEY:tool-server-key}
                    tenant-domains:
                      TESTER-DEV: ALL
                """.formatted(port, moduleName, shortName, serviceName));
        for (String profile : new String[] {"local", "dev", "test", "prod"}) {
            write(root.resolve("src/main/resources/application-" + profile + ".yml"), """
                    spring:
                      config:
                        activate:
                          on-profile: %s
                    """.formatted(profile));
        }
        String manifestSource = toolServiceManifest == null || toolServiceManifest.isBlank() ? """
                mcp:
                  manifest:
                    routing-functions:
                      - name: route_to_%s
                        server-id: %s
                        category-key: %s
                        product-boundary: "업무 범위를 입력하세요."
                        business-domain: "업무 도메인을 입력하세요."
                        business-outcome: "업무 결과를 입력하세요."
                        primary-entities: []
                        capabilities: []
                        select-if: "이 Pod의 업무 요청인 경우"
                        reject-if: "다른 업무 Pod 요청인 경우"
                        confusable-servers: []
                        decision-policy: "업무 도메인을 기준으로 선택합니다."
                """.formatted(moduleName, moduleName, shortName) : toolServiceManifest;
        write(root.resolve("src/main/resources/tool-service-manifest.yml"),
                normalizeToolServiceManifest(manifestSource, moduleName, targetModules));
        write(root.resolve("Dockerfile"), """
                FROM eclipse-temurin:21-jre-alpine
                WORKDIR /app
                COPY build/libs/*-SNAPSHOT.jar app.jar
                EXPOSE %d
                ENTRYPOINT ["java", "-jar", "app.jar"]
                """.formatted(port));
        write(root.resolve("docker-compose.yml"), """
                services:
                  %s:
                    build: .
                    ports:
                      - "${HOST_PORT:%d}:${PORT:%d}"
                    environment:
                      - PORT=${PORT:%d}
                      - AXHUB_TOOL_URL=http://%s:${PORT:%d}
                      - SPRING_PROFILES_ACTIVE=${ACTIVE_PROFILE:-local}
                """.formatted(serviceName, port, port, port, serviceName, port));
        write(root.resolve("README.md"), """
                # %s

                Pod Module New로 생성된 독립 DATMT Tool Pod입니다.

                ## 공통 라이브러리

                `io.shinhanlife:dat-lib-datmt:0.0.1-SNAPSHOT` Maven 좌표로 공통 라이브러리를 참조합니다.
                로컬 개발에서는 먼저 `dat-lib-datmt`에서 `:dat-was-lib:publishToMavenLocal`을 실행한 뒤 이 프로젝트를 빌드합니다.
                신한라이프 이관 후에는 같은 Maven 좌표를 사내 Nexus에서 내려받도록 `mavenLocal()`을 Nexus repository로 교체합니다.

                ## 빌드

                ```powershell
                .\\gradlew.bat compileJava
                ```

                기본 포트는 %d이며, 실행 시 `PORT` 환경변수로 변경할 수 있습니다.
                """.formatted(moduleName, port));
    }

    private static void copyGradleWrapper(Path libraryProject, Path targetProject) throws IOException {
        for (String relativePath : new String[] {
                "gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"}) {
            Path source = libraryProject.resolve(relativePath);
            if (!Files.isRegularFile(source)) {
                throw new IllegalArgumentException("dat-lib-datmt Gradle wrapper file is missing: " + source);
            }
            Path target = targetProject.resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static String pascalCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("-")) {
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @SuppressWarnings("unchecked")
    public static String normalizeToolServiceManifest(String source, String moduleName, List<String> targetModules)
            throws IOException {
        Map<String, Object> root = YAML_MAPPER.readValue(source, Map.class);
        if (!(root.get("mcp") instanceof Map<?, ?> mcp)
                || !(mcp.get("manifest") instanceof Map<?, ?> manifest)
                || !(manifest.get("routing-functions") instanceof List<?> functions)
                || functions.isEmpty()
                || !(functions.getFirst() instanceof Map<?, ?> function)) {
            throw new IOException("tool-service-manifest.yml의 mcp.manifest.routing-functions 형식이 올바르지 않습니다.");
        }
        Map<String, Object> routingFunction = (Map<String, Object>) function;
        routingFunction.put("name", "route_to_" + moduleName);
        routingFunction.put("server-id", moduleName);
        routingFunction.put("category-key", moduleName.substring("dat-was-".length()));
        routingFunction.put("confusable-servers", selectedTargetModules(moduleName, targetModules));
        ((Map<String, Object>) manifest).put("routing-functions", List.of(routingFunction));
        return YAML_MAPPER.writeValueAsString(root);
    }

    private static List<String> selectedTargetModules(String moduleName, List<String> targetModules) {
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        for (String targetModule : targetModules == null ? List.<String>of() : targetModules) {
            if (targetModule != null && !targetModule.equals(moduleName)
                    && MODULE_NAME.matcher(targetModule).matches()) {
                selected.add(targetModule);
            }
        }
        return List.copyOf(selected);
    }

    private static void updateSelectedSiblingManifests(Path workspaceRoot, String moduleName,
                                                       List<String> targetModules) throws IOException {
        for (String targetModule : selectedTargetModules(moduleName, targetModules)) {
            Path manifestPath = workspaceRoot.resolve(targetModule)
                    .resolve("src/main/resources/tool-service-manifest.yml");
            if (!Files.isRegularFile(manifestPath)) {
                continue;
            }
            String before = Files.readString(manifestPath, StandardCharsets.UTF_8);
            String after = addConfusableServer(before, moduleName);
            if (!before.equals(after)) {
                write(manifestPath, after);
            }
        }
    }

    private static String addConfusableServer(String manifest, String moduleName) {
        Matcher matcher = Pattern.compile("(?m)^(\\s*)confusable-servers:\\s*\\[([^]]*)]\\s*$").matcher(manifest);
        if (!matcher.find()) {
            return manifest;
        }
        LinkedHashSet<String> servers = new LinkedHashSet<>();
        for (String value : matcher.group(2).split(",")) {
            String normalized = value.trim();
            if (!normalized.isBlank()) {
                servers.add(normalized);
            }
        }
        if (!servers.add(moduleName)) {
            return manifest;
        }
        String replacement = matcher.group(1) + "confusable-servers: [" + String.join(", ", servers) + "]";
        return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
    }

    private static void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException error) {
                    throw new IllegalStateException(error);
                }
            });
        }
    }
}
