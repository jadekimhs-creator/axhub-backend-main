package io.shinhanlife.dap.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class PodScaffolder {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=========================================");
        System.out.println("   MCP Tool Pod Scaffolder (Java CLI)   ");
        System.out.println("=========================================\n");

        String rawModuleName = getOrAsk(args, 0, scanner, "1. 생성할 모듈(Pod) 이름 (예: payment 또는 dap-tool-payment): ");
        String moduleName = rawModuleName.startsWith("dap-tool-") ? rawModuleName : "dap-tool-" + rawModuleName;
        String portStr = getOrAsk(args, 1, scanner, "2. 사용할 포트 번호 (예: 8085): ");
        String shortName = moduleName.replace("dap-tool-", "").replace("-", "");

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
        String envSourceDir = System.getenv("AXHUB_SOURCE_DIR");
        Path rootDir = envSourceDir != null ? Paths.get(envSourceDir) : Paths.get(".");
        
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
                id 'org.springframework.boot'
            }
            dependencies {
                implementation project(':dap-tool-core')
            }
            dependencies {
                compileOnly 'org.projectlombok:lombok:1.18.32'
                annotationProcessor 'org.projectlombok:lombok:1.18.32'
            }
            """;
        Files.writeString(modulePath.resolve("build.gradle"), buildGradle);

        log.append("[3/6] Dockerfile 생성 중...\n");
        String dockerfile = """
            FROM eclipse-temurin:21-jdk-alpine
            WORKDIR /app
            COPY build/libs/%s-0.0.1-SNAPSHOT.jar app.jar
            ENTRYPOINT ["java", "-jar", "app.jar"]
            """.formatted(moduleName);
        Files.writeString(modulePath.resolve("Dockerfile"), dockerfile);

        log.append("[4/6] Application 클래스 및 설정 파일 생성 중...\n");
        Path srcPath = modulePath.resolve("src/main/java/io/shinhanlife/axhub/biz/mcp/tool/" + shortName);
        Files.createDirectories(srcPath);

        String appClass = """
            package io.shinhanlife.dap.biz.mcp.tool.%s;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
            import org.springframework.cache.annotation.EnableCaching;
            
            /**
             * @package io.shinhanlife.dap.biz.mcp.tool.%s
             * @className DapTool%sApplication
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
            @SpringBootApplication(scanBasePackages = {"io.shinhanlife.dap.biz.mcp.tool", "io.shinhanlife.dap.biz.mcp.adapter", "io.shinhanlife.dap.common.mcp", "io.shinhanlife.dap.common.config"})
            @ConfigurationPropertiesScan(basePackages = {"io.shinhanlife.dap.biz.mcp.tool", "io.shinhanlife.dap.biz.mcp.adapter", "io.shinhanlife.dap.common.mcp", "io.shinhanlife.dap.common.config"})
            @EnableCaching
            public class DapTool%sApplication {
                public static void main(String[] args) {
                    SpringApplication.run(DapTool%sApplication.class, args);
                }
            }
            """.formatted(shortName, shortName, capitalize(shortName), author, createDate, createDate, author, capitalize(shortName), capitalize(shortName));
        Files.writeString(srcPath.resolve("DapTool" + capitalize(shortName) + "Application.java"), appClass);

        Path resPath = modulePath.resolve("src/main/resources");
        Files.createDirectories(resPath);
        String applicationYml = """
            spring:
              config:
                import: "classpath:config/application-glow-local.yml"
            """;
        Files.writeString(resPath.resolve("application-local.yml"), applicationYml);

        String applicationProperties = """
            server.port=%s
            spring.application.name=%s
            
            spring.profiles.active=local
            
            # Suppress Kafka Connection Logs
            logging.level.org.apache.kafka=ERROR
            
            # Auto Prefix Namespace
            mcp.namespace=%s
            
            # API 보안 키 설정
            mcp.security.tenant-domains.TESTER-DEV=ALL
            """.formatted(portStr, moduleName, shortName);
        Files.writeString(resPath.resolve("application.properties"), applicationProperties);

        String applicationLocalProperties = """
            # Local 환경 전용 설정 (H2 메모리 DB 등)
            spring.datasource.url=jdbc:p6spy:h2:mem:testdb;DB_CLOSE_DELAY=-1;
            spring.datasource.driverClassName=com.p6spy.engine.spy.P6SpyDriver
            spring.datasource.username=sa
            spring.datasource.password=password
            
            spring.h2.console.enabled=true
            # EIMS 동적 라우팅 접속 정보
            eims.http.url=http://localhost:${server.port}/api/gateway
            eims.tcp.host=127.0.0.1
            eims.tcp.port=8090
            eims.tcp.timeout=5000
            eims.jsp.form.url=http://localhost:${server.port}/mock/jsp-form
            eims.jsp.json.url=http://localhost:${server.port}/mock/jsp-json
            eims.mci.url=http://localhost:${server.port}/api/mock/esb/api
            eims.mcistring.url=http://localhost:${server.port}/api/mock/esb/string
            
            # Gateway/Tool URLs
            axhub.gateway.url=http://localhost:8081
            axhub.tool.url=http://localhost:${server.port}
            
            # Disable Kafka
            spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
            
            # Suppress Kafka Connection Logs
            logging.level.org.apache.kafka=ERROR
            """;
        Files.writeString(resPath.resolve("application-local.properties"), applicationLocalProperties);

        String applicationDevProperties = """
            # Render 클라우드 환경 전용 설정
            axhub.gateway.url=https://axhub-gateway.onrender.com
            axhub.tool.url=https://%s.onrender.com
            
            # EIMS 동적 라우팅 접속 정보 (Mock)
            eims.http.url=http://localhost:${server.port}/api/gateway
            eims.tcp.host=127.0.0.1
            eims.tcp.port=8090
            eims.tcp.timeout=5000
            eims.jsp.form.url=http://localhost:${server.port}/mock/jsp-form
            eims.jsp.json.url=http://localhost:${server.port}/mock/jsp-json
            eims.mci.url=http://localhost:${server.port}/api/mock/esb/api
            eims.mcistring.url=http://localhost:${server.port}/api/mock/esb/string
            """.formatted(moduleName);
        Files.writeString(resPath.resolve("application-dev.properties"), applicationDevProperties);

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
        Files.writeString(resPath.resolve("logback-spring.xml"), logbackXml);

        log.append("[5/6] settings.gradle 에 모듈 등록 중...\n");
        Path settingsPath = rootDir.resolve(Paths.get("settings.gradle"));
        if (Files.exists(settingsPath)) {
            String settings = Files.readString(settingsPath);
            if (!settings.contains("include '" + moduleName + "'")) {
                Files.writeString(settingsPath, System.lineSeparator() + "include '" + moduleName + "'" + System.lineSeparator(), StandardOpenOption.APPEND);
            }
        }

        log.append("[6/6] docker-compose.yml 에 서비스 추가 중...\n");
        Path dockerComposePath = rootDir.resolve(Paths.get("docker-compose.yml"));
        if (Files.exists(dockerComposePath)) {
            String compose = Files.readString(dockerComposePath);
            String serviceName = moduleName.replace("axhub-", ""); // e.g. tool-payment
            if (!compose.contains("  " + serviceName + ":")) {
                String newService = """
                      %s:
                        build: 
                          context: .
                          dockerfile: %s/Dockerfile
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
                          - GLOW_COMMUNICATION_MCI_HOST=http://mci-mock
                          - GLOW_COMMUNICATION_MCI_PORT=8080
                          - GLOW_COMMUNICATION_EXTMCI_HOST=http://mci-mock
                          - GLOW_COMMUNICATION_EXTMCI_PORT=8080
                          - GLOW_COMMUNICATION_EAI_HOST=http://mci-mock
                          - GLOW_COMMUNICATION_EAI_PORT=8080
                    """.formatted(serviceName, moduleName, portStr, portStr, serviceName, portStr);
                Files.writeString(dockerComposePath, System.lineSeparator() + newService, StandardOpenOption.APPEND);
            }
        }

        log.append("\n=========================================\n");
        log.append("  Pod Scaffolding Complete! \n");
        log.append("=========================================\n");
        log.append("1. [새로운 모듈] ").append(moduleName).append(" 폴더가 생성되었습니다.\n");
        log.append("2. [ToolScaffolder]를 사용해 이 모듈 안에 툴을 추가하세요.\n");
        log.append("3. 실행 전 Gradle 동기화(Sync)를 한 번 진행해 주세요.\n");
        return log.toString();
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
