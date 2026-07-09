package io.shinhanlife.axhub.biz.mcp.tool.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

public class PodScaffolder {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=========================================");
        System.out.println("   MCP Tool Pod Scaffolder (Java CLI)   ");
        System.out.println("=========================================\n");

        String rawModuleName = getOrAsk(args, 0, scanner, "1. 생성할 모듈(Pod) 이름 (예: payment 또는 axhub-tool-payment): ");
        String moduleName = rawModuleName.startsWith("axhub-tool-") ? rawModuleName : "axhub-tool-" + rawModuleName;
        String portStr = getOrAsk(args, 1, scanner, "2. 사용할 포트 번호 (예: 8085): ");
        String shortName = moduleName.replace("axhub-tool-", "").replace("-", "");

        scaffoldPod(moduleName, portStr, shortName);
    }

    private static String getOrAsk(String[] args, int index, Scanner scanner, String prompt) {
        if (args.length > index) {
            return args[index];
        }
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static void scaffoldPod(String moduleName, String portStr, String shortName) throws IOException {
        Path modulePath = Paths.get(moduleName);
        if (Files.exists(modulePath)) {
            System.out.println("[오류] 이미 존재하는 모듈입니다: " + moduleName);
            return;
        }

        System.out.println("\n[1/6] 모듈 디렉터리 생성 중...");
        Files.createDirectories(modulePath);

        System.out.println("[2/6] build.gradle 생성 중...");
        String buildGradle = """
            plugins {
                id 'org.springframework.boot'
            }
            dependencies {
                implementation project(':axhub-tool-core')
            }
            dependencies {
                compileOnly 'org.projectlombok:lombok:1.18.32'
                annotationProcessor 'org.projectlombok:lombok:1.18.32'
            }
            """;
        Files.writeString(modulePath.resolve("build.gradle"), buildGradle);

        System.out.println("[3/6] Dockerfile 생성 중...");
        String dockerfile = """
            FROM eclipse-temurin:21-jdk-alpine
            WORKDIR /app
            COPY build/libs/%s-0.0.1-SNAPSHOT.jar app.jar
            ENTRYPOINT ["java", "-jar", "app.jar"]
            """.formatted(moduleName);
        Files.writeString(modulePath.resolve("Dockerfile"), dockerfile);

        System.out.println("[4/6] Application 클래스 및 설정 파일 생성 중...");
        Path srcPath = modulePath.resolve("src/main/java/io/shinhanlife/axhub/biz/mcp/tool/" + shortName);
        Files.createDirectories(srcPath);

        String appClass = """
            package io.shinhanlife.axhub.biz.mcp.tool.%s;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            import org.springframework.cache.annotation.EnableCaching;
            
            @EnableCaching
            @SpringBootApplication(scanBasePackages = "io.shinhanlife.axhub.biz.mcp")
            public class %sToolApplication {
                public static void main(String[] args) {
                    SpringApplication.run(%sToolApplication.class, args);
                }
            }
            """.formatted(shortName, capitalize(shortName), capitalize(shortName));
        Files.writeString(srcPath.resolve(capitalize(shortName) + "ToolApplication.java"), appClass);

        Path resPath = modulePath.resolve("src/main/resources");
        Files.createDirectories(resPath);
        String applicationYml = """
            spring:
              application:
                name: %s
              config:
                import: "classpath:config/application-glow-local.yml"
            
            server:
              port: %s
              
            axhub:
              tool:
                url: "http://%s:%s"
            """.formatted(moduleName, portStr, moduleName.replace("axhub-backend-main-", "").replace("axhub-", ""), portStr);
        Files.writeString(resPath.resolve("application-local.yml"), applicationYml);

        System.out.println("[5/6] settings.gradle 에 모듈 등록 중...");
        Path settingsPath = Paths.get("settings.gradle");
        if (Files.exists(settingsPath)) {
            String settings = Files.readString(settingsPath);
            if (!settings.contains("include '" + moduleName + "'")) {
                Files.writeString(settingsPath, System.lineSeparator() + "include '" + moduleName + "'" + System.lineSeparator(), StandardOpenOption.APPEND);
            }
        }

        System.out.println("[6/6] docker-compose.yml 에 서비스 추가 중...");
        Path dockerComposePath = Paths.get("docker-compose.yml");
        if (Files.exists(dockerComposePath)) {
            String compose = Files.readString(dockerComposePath);
            String serviceName = moduleName.replace("axhub-", ""); // e.g. tool-payment
            if (!compose.contains("  " + serviceName + ":")) {
                String newService = """
                      %s:
                        build: 
                          context: ./%s
                        ports:
                          - "%s:%s"
                        depends_on:
                          - redis
                        environment:
                          - TZ=Asia/Seoul
                          - SPRING_REDIS_HOST=redis
                          - SPRING_REDIS_PORT=6379
                          - SPRING_DATA_REDIS_PORT=6379
                          - AXHUB_GATEWAY_URL=http://gateway:8081
                          - AXHUB_TOOL_URL=http://%s:%s
                          - GLOW_COMMUNICATION_MCI_HOST=http://gateway
                          - GLOW_COMMUNICATION_MCI_PORT=8081
                          - GLOW_COMMUNICATION_EXTMCI_HOST=http://gateway
                          - GLOW_COMMUNICATION_EXTMCI_PORT=8081
                          - GLOW_COMMUNICATION_EAI_HOST=tcp://gateway
                          - GLOW_COMMUNICATION_EAI_PORT=8081
                    """.formatted(serviceName, moduleName, portStr, portStr, serviceName, portStr);
                Files.writeString(dockerComposePath, System.lineSeparator() + newService, StandardOpenOption.APPEND);
            }
        }

        System.out.println("\\n=========================================");
        System.out.println(" 🎉 Pod Scaffolding Complete! ");
        System.out.println("=========================================");
        System.out.println("1. [새로운 모듈] " + moduleName + " 폴더가 생성되었습니다.");
        System.out.println("2. [ToolScaffolder]를 사용해 이 모듈 안에 툴을 추가하세요.");
        System.out.println("3. 실행 전 Gradle 동기화(Sync)를 한 번 진행해 주세요.");
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
