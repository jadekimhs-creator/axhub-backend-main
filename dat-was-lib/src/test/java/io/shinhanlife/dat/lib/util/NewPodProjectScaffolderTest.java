package io.shinhanlife.dat.lib.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NewPodProjectScaffolderTest {

    @TempDir
    Path workspace;

    @Test
    void createsStandaloneProjectWithMavenLibraryDependency() throws Exception {
        createLibraryProject();
        String result = NewPodProjectScaffolder.scaffold(workspace, "dat-was-payment", 8099,
                "tester", "2026.09.09");

        Path project = workspace.resolve("dat-was-payment");
        assertTrue(Files.exists(project.resolve("settings.gradle")));
        assertTrue(Files.exists(project.resolve("build.gradle")));
        assertTrue(Files.exists(project.resolve("src/main/resources/application.yml")));
        assertTrue(Files.exists(project.resolve("docker-compose.yml")));
        assertTrue(!Files.exists(project.resolve("k8s")));
        assertTrue(Files.exists(project.resolve("gradlew.bat")));
        assertTrue(Files.exists(project.resolve("gradle/wrapper/gradle-wrapper.jar")));
        String settings = Files.readString(project.resolve("settings.gradle"));
        String build = Files.readString(project.resolve("build.gradle"));
        assertTrue(!settings.contains("includeBuild('../dat-lib-datmt')"));
        assertTrue(build.contains("mavenLocal()"));
        assertTrue(build.contains("io.shinhanlife:dat-lib-datmt:0.0.1-SNAPSHOT"));
        assertTrue(build.contains("publishToMavenLocal"));
        assertTrue(Files.readString(project.resolve("README.md")).contains("publishToMavenLocal"));
        assertTrue(!Files.readString(project.resolve("README.md")).contains("composite build"));
        assertTrue(Files.readString(project.resolve("src/main/resources/application.yml"))
                .contains("port: ${PORT:8099}"));
        assertTrue(Files.readString(project.resolve("src/main/resources/application.yml"))
                .contains("import: optional:classpath:tool-service-manifest.yml"));
        assertTrue(result.contains("dat-was-payment"));
    }

    @Test
    void createsModuleInsideSelectedPodProjectRoot() throws Exception {
        createLibraryProject();
        createCustomerPodTemplate();
        Path podProjectRoot = workspace.resolve("dat-was-datsu");
        Files.createDirectories(podProjectRoot);

        NewPodProjectScaffolder.scaffold(podProjectRoot, "dat-was-sup", 8087,
                "tester", "2026.09.11");

        assertTrue(Files.isRegularFile(podProjectRoot.resolve("settings.gradle")));
        assertTrue(Files.isRegularFile(podProjectRoot.resolve("gradlew.bat")));
        assertTrue(Files.isRegularFile(podProjectRoot.resolve("dat-was-sup/build.gradle")));
        assertTrue(Files.isRegularFile(podProjectRoot.resolve(
                "dat-was-sup/src/main/resources/tool-service-manifest.yml")));
        assertEquals("""
                        rootProject.name = 'dat-was-datsu'
                        // CU settings Gradle format
                        include 'dat-was-sup'
                        """, Files.readString(podProjectRoot.resolve("settings.gradle")));
        assertTrue(Files.readString(podProjectRoot.resolve("build.gradle"))
                .equals("plugins { id 'java' }\n// CU root Gradle format\n"));
        assertTrue(Files.readString(podProjectRoot.resolve("dat-was-sup/build.gradle"))
                .equals("plugins { id 'org.springframework.boot' }\n// CU module Gradle format\n"));
        assertTrue(!Files.exists(podProjectRoot.resolve("k8s")));
        assertTrue(!Files.exists(workspace.resolve("dat-was-sup")));
    }

    @Test
    void writesAiManifestToNewPodAndUpdatesSelectedSiblingPod() throws Exception {
        createLibraryProject();
        Path siblingManifest = workspace.resolve("dat-was-customer/src/main/resources/tool-service-manifest.yml");
        Files.createDirectories(siblingManifest.getParent());
        Files.writeString(siblingManifest, """
                mcp:
                  manifest:
                    routing-functions:
                      - name: route_to_dat-was-customer
                        server-id: dat-was-customer
                        category-key: customer
                        confusable-servers: []
                """);
        String aiManifest = """
                mcp:
                  manifest:
                    routing-functions:
                      - name: wrong_name
                        server-id: wrong-server
                        category-key: wrong
                        product-boundary: "보험"
                        business-domain: "납부 관리"
                        business-outcome: "보험료 납부 업무를 처리합니다."
                        primary-entities: ["보험료"]
                        capabilities: ["납부 조회"]
                        select-if: "보험료 납부를 요청한 경우"
                        reject-if: "고객 정보만 조회하는 경우"
                        confusable-servers: []
                        decision-policy: "납부 업무를 우선 선택합니다."
                """;

        NewPodProjectScaffolder.scaffold(workspace, "dat-was-payment", 8099, "tester", "2026.09.11",
                aiManifest, List.of("dat-was-customer"));

        String newManifest = Files.readString(workspace.resolve(
                "dat-was-payment/src/main/resources/tool-service-manifest.yml"));
        assertTrue(newManifest.contains("name: \"route_to_dat-was-payment\""), newManifest);
        assertTrue(newManifest.contains("server-id: \"dat-was-payment\""), newManifest);
        assertTrue(newManifest.contains("- \"dat-was-customer\""), newManifest);
        assertTrue(Files.readString(siblingManifest).contains("dat-was-payment"));
    }

    private void createLibraryProject() throws Exception {
        Path library = workspace.resolve("dat-lib-datmt");
        Files.createDirectories(library.resolve("gradle/wrapper"));
        Files.writeString(library.resolve("gradlew"), "#!/bin/sh\n");
        Files.writeString(library.resolve("gradlew.bat"), "@echo off\n");
        Files.writeString(library.resolve("gradle/wrapper/gradle-wrapper.jar"), "wrapper");
        Files.writeString(library.resolve("gradle/wrapper/gradle-wrapper.properties"), "distributionUrl=test\n");
    }

    private void createCustomerPodTemplate() throws Exception {
        Path template = workspace.resolve("dat-was-datcu");
        Files.createDirectories(template.resolve("dat-was-cus"));
        Files.writeString(template.resolve("settings.gradle"), """
                rootProject.name = 'dat-was-datcu'
                // CU settings Gradle format
                include 'dat-was-cus'
                """);
        Files.writeString(template.resolve("build.gradle"), "plugins { id 'java' }\n// CU root Gradle format\n");
        Files.writeString(template.resolve("dat-was-cus/build.gradle"),
                "plugins { id 'org.springframework.boot' }\n// CU module Gradle format\n");
    }

    @Test
    void rejectsInvalidNamesPortsAndExistingProjects() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> NewPodProjectScaffolder.scaffold(workspace, "payment", 8099, "tester", "2026.09.09"));
        assertThrows(IllegalArgumentException.class,
                () -> NewPodProjectScaffolder.scaffold(workspace, "dat-was-lib", 8099, "tester", "2026.09.09"));
        assertThrows(IllegalArgumentException.class,
                () -> NewPodProjectScaffolder.scaffold(workspace, "dat-was-payment", 0, "tester", "2026.09.09"));
        Files.createDirectories(workspace.resolve("dat-was-payment"));
        assertThrows(IllegalStateException.class,
                () -> NewPodProjectScaffolder.scaffold(workspace, "dat-was-payment", 8099, "tester", "2026.09.09"));
    }
}
