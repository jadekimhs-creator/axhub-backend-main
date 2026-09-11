# Pod Module New Workspace Scaffolder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 기존 Pod Scaffold를 변경하지 않고, Scaffold 화면에서 독립 Tool Pod 프로젝트를 `C:\eGovFrameDev-4.3.1-64bit\workspace`에 생성한다.

**Architecture:** 기존 `PodScaffolder`와 `/api/v1/scaffold/pod`는 유지한다. `NewPodProjectScaffolder`가 독립 Gradle 프로젝트 파일을 생성하고, 별도 `/api/v1/scaffold/pod-new` endpoint와 `Pod Module New` UI가 이를 호출한다. 생성 프로젝트는 `../dat-lib-datmt` composite build로 공통 라이브러리를 해석한다.

**Tech Stack:** Java 21, Spring Boot 3.5.11, Gradle composite build, JUnit 5, MockMvc, static HTML/JavaScript.

**Spec:** `docs/superpowers/specs/2026-09-09-new-pod-module-workspace-scaffolder-design.md`

## Global Constraints

- `PodScaffolder` 및 기존 `/api/v1/scaffold/pod`의 코드와 동작을 변경하지 않는다.
- 기존 Pod 모듈 `dat-was-cus`, `dat-was-pro`, `dat-was-sal`, `dat-was-sys`를 수정하지 않는다.
- 신규 생성 대상은 사용자 입력 workspace의 `<workspace>/<moduleName>`이며, 기본 workspace는 `C:\eGovFrameDev-4.3.1-64bit\workspace`이다.
- moduleName은 `dat-was-` 접두사가 붙은 소문자·숫자·하이픈 식별자만 허용하고 `dat-was-lib`는 거부한다.
- port는 1부터 65535까지의 정수만 허용한다.
- 생성 프로젝트는 `../dat-lib-datmt`를 포함하고 `io.shinhanlife:dat-lib-datmt:0.0.1-SNAPSHOT`를 `:dat-was-lib`로 치환한다.
- 사용자 요청이 없는 한 commit 또는 push하지 않는다.

---

### Task 1: 독립 Pod 프로젝트 생성기

**Files:**
- Create: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/NewPodProjectScaffolder.java`
- Create: `dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/NewPodProjectScaffolderTest.java`

**Interfaces:**
- Produces: `NewPodProjectScaffolder.scaffold(Path workspaceRoot, String moduleName, int port, String author, String createdDate)` returning a human-readable generation summary.
- Produces: `NewPodProjectScaffolder.validateModuleName(String moduleName)` and `validatePort(int port)` throwing `IllegalArgumentException` for invalid input.
- Consumes: a writable workspace root; creates no files outside it.

- [ ] **Step 1: Write the failing generator test**

```java
@Test
void createsStandaloneProjectWithCompositeBuild() throws Exception {
    String result = NewPodProjectScaffolder.scaffold(workspace, "dat-was-payment", 8099,
            "tester", "2026.09.09");

    Path project = workspace.resolve("dat-was-payment");
    assertTrue(Files.exists(project.resolve("settings.gradle")));
    assertTrue(Files.exists(project.resolve("build.gradle")));
    assertTrue(Files.exists(project.resolve("src/main/resources/application.yml")));
    assertTrue(Files.exists(project.resolve("docker-compose.yml")));
    assertTrue(Files.exists(project.resolve("k8s/base/deployment.yaml")));
    assertTrue(Files.readString(project.resolve("settings.gradle"))
            .contains("includeBuild('../dat-lib-datmt')"));
    assertTrue(Files.readString(project.resolve("build.gradle"))
            .contains("io.shinhanlife:dat-lib-datmt:0.0.1-SNAPSHOT"));
    assertTrue(Files.readString(project.resolve("src/main/resources/application.yml"))
            .contains("port: ${PORT:8099}"));
    assertTrue(result.contains("dat-was-payment"));
}
```

- [ ] **Step 2: Run the new test and verify the expected failure**

Run: `.\gradlew.bat :dat-was-lib:test --tests "io.shinhanlife.dat.lib.util.NewPodProjectScaffolderTest.createsStandaloneProjectWithCompositeBuild" --no-daemon`

Expected: compilation failure because `NewPodProjectScaffolder` does not exist.

- [ ] **Step 3: Write failing validation and non-overwrite tests**

```java
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
```

- [ ] **Step 4: Run validation test and verify the expected failure**

Run: `.\gradlew.bat :dat-was-lib:test --tests "io.shinhanlife.dat.lib.util.NewPodProjectScaffolderTest.rejectsInvalidNamesPortsAndExistingProjects" --no-daemon`

Expected: compilation failure because the new generator is not implemented.

- [ ] **Step 5: Implement the generator**

Create only the requested standalone project root. Generate these files:

```text
<workspace>/<moduleName>/settings.gradle
<workspace>/<moduleName>/build.gradle
<workspace>/<moduleName>/README.md
<workspace>/<moduleName>/Dockerfile
<workspace>/<moduleName>/docker-compose.yml
<workspace>/<moduleName>/k8s/base/deployment.yaml
<workspace>/<moduleName>/k8s/base/service.yaml
<workspace>/<moduleName>/src/main/java/io/shinhanlife/dat/mcc/<shortName>/DatWas<PascalName>Application.java
<workspace>/<moduleName>/src/main/resources/application.yml
<workspace>/<moduleName>/src/main/resources/application-local.yml
<workspace>/<moduleName>/src/main/resources/application-dev.yml
<workspace>/<moduleName>/src/main/resources/application-test.yml
<workspace>/<moduleName>/src/main/resources/application-prod.yml
<workspace>/<moduleName>/src/main/resources/tool-service-manifest.yml
```

Use this generated `settings.gradle` content:

```gradle
rootProject.name = '<moduleName>'

includeBuild('../dat-lib-datmt') {
    dependencySubstitution {
        substitute module('io.shinhanlife:dat-lib-datmt') using project(':dat-was-lib')
    }
}
```

Use this generated dependency declaration:

```gradle
dependencies {
    implementation 'io.shinhanlife:dat-lib-datmt:0.0.1-SNAPSHOT'
}
```

Make all writes UTF-8 and use `Files.createDirectories` only after validation. Build a temporary project directory next to the final target and move it into place atomically after all writes succeed; remove that temporary directory if any write fails.

- [ ] **Step 6: Run generator tests and verify they pass**

Run: `.\gradlew.bat :dat-was-lib:test --tests "io.shinhanlife.dat.lib.util.NewPodProjectScaffolderTest" --no-daemon`

Expected: all new generator tests pass.

### Task 2: 신규 Scaffold API

**Files:**
- Modify: `dat-gateway/src/main/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingController.java`
- Modify: `dat-gateway/src/test/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingControllerToolDraftTest.java`

**Interfaces:**
- Consumes: JSON `{"moduleName":"dat-was-payment","port":8099,"author":"tester","workspacePath":"C:\\temp\\workspace"}`.
- Produces: `POST /api/v1/scaffold/pod-new` response with HTTP 200 and generated-project summary, or HTTP 400 with a validation message.
- Consumes: `NewPodProjectScaffolder.scaffold(Path, String, int, String, String)` from Task 1.

- [ ] **Step 1: Write the failing MockMvc success test**

```java
@Test
void createsStandalonePodProjectWithoutCallingExistingPodScaffolder() throws Exception {
    mockMvc.perform(post("/api/v1/scaffold/pod-new")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"moduleName":"dat-was-payment","port":8099,
                             "author":"tester","workspacePath":"%s"}
                            """.formatted(root.toString().replace("\\", "\\\\"))))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("dat-was-payment")));

    assertTrue(Files.exists(root.resolve("dat-was-payment/settings.gradle")));
}
```

- [ ] **Step 2: Run the API test and verify the expected failure**

Run: `.\gradlew.bat :dat-gateway:test --tests "io.shinhanlife.dat.mcg.presentation.ScaffoldingControllerToolDraftTest.createsStandalonePodProjectWithoutCallingExistingPodScaffolder" --no-daemon`

Expected: HTTP 404 because `/api/v1/scaffold/pod-new` does not exist.

- [ ] **Step 3: Add the isolated endpoint and request validation**

Add `@PostMapping("/pod-new")` without altering `scaffoldPod`. Define a `NewPodProjectRequest` record in `ScaffoldingController` with `moduleName`, `Integer port`, `author`, and `workspacePath`. Default a missing or blank workspace path only for this new endpoint to `C:\eGovFrameDev-4.3.1-64bit\workspace`. Return `ResponseEntity.badRequest()` for invalid names, missing ports, invalid ports, or a pre-existing target path. Do not set `AXHUB_SOURCE_DIR` in this endpoint.

- [ ] **Step 4: Write and run the duplicate-target API test**

```java
@Test
void rejectsExistingStandalonePodProject() throws Exception {
    Files.createDirectories(root.resolve("dat-was-payment"));

    mockMvc.perform(post("/api/v1/scaffold/pod-new")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"moduleName":"dat-was-payment","port":8099,
                             "workspacePath":"%s"}
                            """.formatted(root.toString().replace("\\", "\\\\"))))
            .andExpect(status().isBadRequest());
}
```

Run: `.\gradlew.bat :dat-gateway:test --tests "io.shinhanlife.dat.mcg.presentation.ScaffoldingControllerToolDraftTest" --no-daemon`

Expected: all controller scaffolding tests pass, including existing `/pod` behavior.

### Task 3: Pod Module New 화면

**Files:**
- Modify: `dat-gateway/src/main/resources/static/admin/scaffold.html`
- Test: `dat-gateway/src/test/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingControllerToolDraftTest.java`

**Interfaces:**
- Consumes: `/api/v1/scaffold/pod-new` from Task 2.
- Produces: a separate `newPodForm`; no existing `podForm` markup, `handleFormSubmit('podForm', ...)`, or existing manifest draft request changes.

- [ ] **Step 1: Write a failing static-page assertion**

```java
@Test
void scaffoldPageContainsSeparateNewPodForm() throws Exception {
    String page = Files.readString(Path.of("src/main/resources/static/admin/scaffold.html"));
    assertTrue(page.contains("Pod Module New"));
    assertTrue(page.contains("id=\"newPodForm\""));
    assertTrue(page.contains("/api/v1/scaffold/pod-new"));
    assertTrue(page.contains("C:\\eGovFrameDev-4.3.1-64bit\\workspace"));
}
```

- [ ] **Step 2: Run the assertion and verify the expected failure**

Run: `.\gradlew.bat :dat-gateway:test --tests "io.shinhanlife.dat.mcg.presentation.ScaffoldingControllerToolDraftTest.scaffoldPageContainsSeparateNewPodForm" --no-daemon`

Expected: assertion failure because the new tab and form do not exist.

- [ ] **Step 3: Add a separate tab and form**

Add a new `Pod Module New` tab after the existing Pod Module tab. Its `newPodForm` must have only workspace path, module name, service port, author, and date inputs. Pre-fill only this new form's workspace path with `C:\eGovFrameDev-4.3.1-64bit\workspace`. Bind it with `handleFormSubmit('newPodForm', '/api/v1/scaffold/pod-new')`. Keep `podForm`, its workspace default, and its `/pod` handler unchanged.

- [ ] **Step 4: Run the static-page assertion and controller tests**

Run: `.\gradlew.bat :dat-gateway:test --tests "io.shinhanlife.dat.mcg.presentation.ScaffoldingControllerToolDraftTest" --no-daemon`

Expected: all scaffold controller tests pass.

### Task 4: End-to-end generated project verification

**Files:**
- Modify: `README.md`

**Interfaces:**
- Documents: `Pod Module New` creates an independent repository under the selected workspace, while existing Pod Module continues to create modules under its selected root.

- [ ] **Step 1: Add a Korean README section**

Document the two Scaffold paths with this behavior table:

```markdown
| 기능 | 생성 위치 | 기존 Pod 영향 |
| --- | --- | --- |
| Pod Module | 선택한 기존 프로젝트 root | 기존 동작 유지 |
| Pod Module New | `C:\eGovFrameDev-4.3.1-64bit\workspace\<moduleName>` | 기존 Pod/Scaffolder 미수정 |
```

State that `Pod Module New` requires `../dat-lib-datmt` to exist beside the generated project and that the generated port comes from the form input, overridable by `PORT` at runtime.

- [ ] **Step 2: Create one temporary project through the new endpoint**

Use a unique throwaway module name such as `dat-was-scaffoldverify` and workspace `C:\eGovFrameDev-4.3.1-64bit\workspace`. Confirm the generated directory contains the settings file, composite dependency substitution, application source, and Docker/Kubernetes files.

- [ ] **Step 3: Compile the generated project**

Run from the generated project:

```powershell
.\gradlew.bat compileJava --no-daemon
```

Expected: Gradle resolves `../dat-lib-datmt/:dat-was-lib` and exits successfully.

- [ ] **Step 4: Remove only the verified throwaway project**

Verify the absolute path is exactly `C:\eGovFrameDev-4.3.1-64bit\workspace\dat-was-scaffoldverify`, then remove that one generated verification project. Do not alter any user-created project under `workspace`.

- [ ] **Step 5: Run final targeted verification**

Run:

```powershell
.\gradlew.bat :dat-was-lib:test --tests "io.shinhanlife.dat.lib.util.NewPodProjectScaffolderTest" --no-daemon
.\gradlew.bat :dat-gateway:test --tests "io.shinhanlife.dat.mcg.presentation.ScaffoldingControllerToolDraftTest" --no-daemon
git diff --check
```

Expected: both focused test classes pass and `git diff --check` reports no whitespace errors.
