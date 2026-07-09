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

        String author = getOrAsk(args, 2, scanner, "3. 작성자 (예: 김형식): ");
        if (author.trim().isEmpty()) author = "김형식";
        String createDate = getOrAsk(args, 3, scanner, "4. 작성일 (예: 2026.09.01): ");
        if (createDate.trim().isEmpty()) createDate = "2026.09.01";

        scaffoldPod(moduleName, portStr, shortName, author, createDate);
    }

    private static String getOrAsk(String[] args, int index, Scanner scanner, String prompt) {
        if (args.length > index) {
            return args[index];
        }
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static void scaffoldPod(String moduleName, String portStr, String shortName, String author, String createDate) throws IOException {
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
            
            /**
             * @package io.shinhanlife.axhub.biz.mcp.tool.%s
             * @className %sToolApplication
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
            @SpringBootApplication(scanBasePackages = {"io.shinhanlife.axhub.biz.mcp.tool", "io.shinhanlife.axhub.biz.mcp.adapter", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"})
            @org.springframework.boot.context.properties.ConfigurationPropertiesScan(basePackages = {"io.shinhanlife.axhub.biz.mcp.tool", "io.shinhanlife.axhub.biz.mcp.adapter", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"})
            @EnableCaching
            public class %sToolApplication {
                public static void main(String[] args) {
                    SpringApplication.run(%sToolApplication.class, args);
                }
            }
            """.formatted(shortName, shortName, capitalize(shortName), author, createDate, createDate, author, capitalize(shortName), capitalize(shortName));
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

        String applicationProperties = """
            spring.profiles.active=local
            mcp.namespace=%s
            """.formatted(shortName);
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
            
            # API 보안 키 설정
            mcp.security.api-keys.SHINHAN_MCP_SECRET_KEY_2026=mcp-client-1
            mcp.security.api-keys.SHINHAN_MCP_TEST_KEY_9999=mcp-client-2
            mcp.security.tenant-domains.mcp-client-1=CUSTOMER,COMMON
            mcp.security.tenant-domains.mcp-client-2=ALL
            
            # Gateway/Tool URLs
            axhub.gateway.url=http://localhost:8081
            axhub.tool.url=http://localhost:${server.port}
            
            # Disable Kafka
            spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
            
            # Suppress Kafka Connection Logs
            logging.level.org.apache.kafka=ERROR
            """;
        Files.writeString(resPath.resolve("application-local.properties"), applicationLocalProperties);

        String logbackXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <configuration>
                <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n" />
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
