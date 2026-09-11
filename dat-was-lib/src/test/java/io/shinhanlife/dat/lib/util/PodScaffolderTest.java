package io.shinhanlife.dat.lib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PodScaffolderTest {

    @TempDir
    Path root;

    @Test
    void generatesBuildGradleUsingRepositoryStandard() throws Exception {
        PodScaffolder.scaffoldPod(root, "dat-was-sample", "8099", "sample", "tester", "2026.08.07");

        String buildGradle = Files.readString(root.resolve("dat-was-sample/build.gradle"));
        assertTrue(buildGradle.contains("id 'org.springframework.boot'"));
        assertTrue(buildGradle.contains("Spring Boot 3.5.11"));
        assertTrue(buildGradle.contains("implementation project(':dat-was-lib')"));
        assertFalse(buildGradle.contains("compileOnly 'org.projectlombok:lombok"));
    }

    @Test
    void generatesOthStandardRuntimeResources() throws Exception {
        PodScaffolder.scaffoldPod(root, "dat-was-sample", "8099", "sample", "tester", "2026.08.07");

        Path resources = root.resolve("dat-was-sample/src/main/resources");
        String dockerfile = Files.readString(root.resolve("dat-was-sample/Dockerfile"));

        assertTrue(dockerfile.contains("RUN apk add --no-cache tzdata"));
        assertTrue(dockerfile.contains("ENV TZ=Asia/Seoul"));
        assertTrue(dockerfile.contains("COPY dat-was-sample/build/libs/*-SNAPSHOT.jar app.jar"));
        assertTrue(dockerfile.contains("EXPOSE 8099"));
        assertTrue(Files.exists(resources.resolve("application-test.yml")));
        assertTrue(Files.exists(resources.resolve("application-prod.yml")));
        assertTrue(Files.readString(resources.resolve("application-local.yml"))
                .contains("classpath:glow/application-glow-local.yml"));
        assertFalse(Files.readString(resources.resolve("application-local.yml"))
                .contains("application-core-local.yml"));
        assertTrue(Files.readString(resources.resolve("application-dev.yml"))
                .contains("classpath:glow/application-glow-dev.yml"));
        assertTrue(Files.readString(resources.resolve("application-dev.yml"))
                .contains("classpath:application-core-dev.yml"));
        assertTrue(Files.readString(resources.resolve("application-test.yml"))
                .contains("classpath:glow/application-glow-test.yml"));
        assertTrue(Files.readString(resources.resolve("application-test.yml"))
                .contains("classpath:application-core-test.yml"));
        assertTrue(Files.readString(resources.resolve("application-prod.yml"))
                .contains("classpath:glow/application-glow-prod.yml"));
        assertTrue(Files.readString(resources.resolve("application-prod.yml"))
                .contains("classpath:application-core-prod.yml"));
    }

    @Test
    void generatesOnlyProfileImportsInEnvironmentFiles() throws Exception {
        PodScaffolder.scaffoldPod(root, "dat-was-cla", "8087", "cla", "tester", "2026.09.02");

        Path resources = root.resolve("dat-was-cla/src/main/resources");
        for (String profile : List.of("dev", "test", "prod")) {
            String content = Files.readString(resources.resolve("application-" + profile + ".yml"));
            assertTrue(content.contains("port: ${PORT:8087}"));
            assertTrue(content.contains("on-profile: " + profile));
            assertTrue(content.contains("classpath:application-core-" + profile + ".yml"));
            assertTrue(content.contains("classpath:glow/application-glow.yml"));
            assertTrue(content.contains("classpath:glow/application-glow-" + profile + ".yml"));
            assertFalse(content.contains("axhub:"));
            assertFalse(content.contains("eims:"));
            assertFalse(content.contains("shinhan:"));
        }
    }

    @Test
    void generatesLocalProfileWithOnlyGlowImports() throws Exception {
        PodScaffolder.scaffoldPod(root, "dat-was-cla", "8087", "cla", "tester", "2026.09.03");

        String localProfile = Files.readString(root.resolve("dat-was-cla/src/main/resources/application-local.yml"));

        assertEquals("""
                spring:
                  config:
                    activate:
                      on-profile: local
                    import:
                      - classpath:glow/application-glow.yml
                      - classpath:glow/application-glow-local.yml
                """, localProfile);
    }

    @Test
    void canonicalizesEhrManifestAndUsesAllExistingPodsAsConfusableServers() throws Exception {
        Files.writeString(root.resolve("docker-compose.yml"), "services:\n");
        Files.createDirectories(root.resolve("dat-was-sal"));
        Files.createDirectories(root.resolve("dat-was-pro"));
        Files.createDirectories(root.resolve("dat-was-sys"));
        String requestedManifest = """
                mcp:
                  manifest:
                    routing-functions:
                      route_to_ehr:
                        name: route_to_ehr
                        server-id: ehr-service
                        category-key: ehr
                        select-if: 청구 또는 지급 업무 요청인 경우
                        reject-if: 고객 상담이 주된 요청인 경우
                        confusable-servers: [ehr-backend, ehr-core]
                """;

        PodScaffolder.scaffoldPod(root, "dat-was-ehr", "8087", "ehr", "tester", "2026.09.03", requestedManifest);

        String manifest = Files.readString(root.resolve("dat-was-ehr/src/main/resources/tool-service-manifest.yml"));
        Map<String, Object> routingFunction = routingFunction(manifest);
        assertEquals("route_to_dat-was-ehr", routingFunction.get("name"));
        assertEquals("dat-was-ehr", routingFunction.get("server-id"));
        assertEquals("ehr", routingFunction.get("category-key"));
        assertEquals("청구 또는 지급 업무 요청인 경우", routingFunction.get("select-if"));
        assertEquals("고객 상담이 주된 요청인 경우", routingFunction.get("reject-if"));
        assertEquals(List.of("dat-was-pro", "dat-was-sal", "dat-was-sys"), routingFunction.get("confusable-servers"));

        String compose = Files.readString(root.resolve("docker-compose.yml"));
        assertTrue(compose.contains("  was-ehr:"));
        assertFalse(compose.contains("  dat-was-ehr:"));
        assertTrue(compose.contains("SPRING_PROFILES_ACTIVE=${ACTIVE_PROFILE:-local}"));
    }

    @Test
    void normalizesAiManifestWhenAnUnquotedKoreanDescriptionContainsColon() throws Exception {
        String aiManifest = """
                mcp:
                  manifest:
                    routing-functions:
                      - name: route_to_dat-was-ehr
                        server-id: dat-was-ehr
                        category-key: ehr
                        business-outcome: 인사 정보와 휴가 정보를 제공합니다.
                        reject-if: 인사 운영의 핵심 기능(예: 급여, 승진, 보상)과 관련된 경우 다른 서버로 전달합니다.
                        confusable-servers: [dat-was-hrd, dat-was-pay]
                """;

        String normalized = PodScaffolder.normalizeToolServiceManifest(aiManifest, "dat-was-ehr",
                List.of("dat-was-cus", "dat-was-ehr", "dat-was-sal"));
        Map<String, Object> routingFunction = routingFunction(normalized);

        assertEquals("인사 운영의 핵심 기능(예: 급여, 승진, 보상)과 관련된 경우 다른 서버로 전달합니다.",
                routingFunction.get("reject-if"));
        assertEquals(List.of("dat-was-cus", "dat-was-sal"), routingFunction.get("confusable-servers"));
    }

    @Test
    void normalizesAiManifestWithDottedRoutingFunctionsRoot() throws Exception {
        String aiManifest = """
                mcp.manifest.routing-functions:
                  - name: route_to_dat-was-ehr
                    server-id: dat-was-ehr
                    category-key: ehr
                    business-outcome: "인사 정보와 휴가 정보를 제공합니다."
                    confusable-servers: [dat-was-hrd, dat-was-pay]
                """;

        String normalized = PodScaffolder.normalizeToolServiceManifest(aiManifest, "dat-was-ehr",
                List.of("dat-was-cus", "dat-was-ehr", "dat-was-sal"));
        Map<String, Object> routingFunction = routingFunction(normalized);

        assertEquals("route_to_dat-was-ehr", routingFunction.get("name"));
        assertEquals(List.of("dat-was-cus", "dat-was-sal"), routingFunction.get("confusable-servers"));
    }

    @Test
    void generatesRedisOptionalPodComposeConfiguration() throws Exception {
        Files.writeString(root.resolve("settings.gradle"), "rootProject.name = 'test'\n");
        Files.writeString(root.resolve("docker-compose.yml"), "services:\n");

        PodScaffolder.scaffoldPod(root, "dat-was-pro", "8085", "pro", "tester", "2026.08.13");

        String compose = Files.readString(root.resolve("docker-compose.yml"));
        assertTrue(compose.contains("was-pro:"));
        assertFalse(compose.contains("depends_on:\n      - redis"));
        assertFalse(compose.contains("SPRING_REDIS_HOST=redis"));
    }

    @Test
    void addsNewPodToReferencedExistingConfusableServerManifests() throws Exception {
        Path proManifest = existingManifest("dat-was-pro", "dat-was-cus");
        Path cusManifest = existingManifest("dat-was-cus", "dat-was-pro");
        String newManifest = """
                mcp:
                  manifest:
                    routing-functions:
                      - name: route_to_dat-was-cla
                        server-id: dat-was-cla
                        category-key: cla
                        confusable-servers:
                          - dat-was-pro
                          - dat-was-cus
                          - dat-was-missing
                          - dat-was-cla
                """;

        String result = PodScaffolder.scaffoldPod(root, "dat-was-cla", "8098", "cla", "tester",
                "2026.09.02", newManifest);

        assertTrue(Files.readString(proManifest).contains("confusable-servers: [dat-was-cus, dat-was-cla]"));
        assertTrue(Files.readString(cusManifest).contains("confusable-servers: [dat-was-pro, dat-was-cla]"));
        assertFalse(Files.exists(root.resolve("dat-was-missing")));
        assertTrue(result.contains("dat-was-pro/src/main/resources/tool-service-manifest.yml"), result);
        assertTrue(result.contains("dat-was-cus/src/main/resources/tool-service-manifest.yml"), result);
    }

    private Path existingManifest(String moduleName, String confusableServer) throws Exception {
        Path path = root.resolve(moduleName + "/src/main/resources/tool-service-manifest.yml");
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
                mcp:
                  manifest:
                    routing-functions:
                      - name: route_to_%s
                        server-id: %s
                        category-key: %s
                        confusable-servers: [%s]
                """.formatted(moduleName, moduleName, moduleName.replace("dat-was-", ""), confusableServer));
        return path;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> routingFunction(String manifest) throws Exception {
        Map<String, Object> root = new ObjectMapper(new YAMLFactory()).readValue(manifest, Map.class);
        Map<String, Object> mcp = (Map<String, Object>) root.get("mcp");
        Map<String, Object> manifestNode = (Map<String, Object>) mcp.get("manifest");
        return (Map<String, Object>) ((List<?>) manifestNode.get("routing-functions")).getFirst();
    }
}
