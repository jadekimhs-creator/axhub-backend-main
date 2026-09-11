# Tool Schema V17 Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** BC-DAB-STD-003 V17의 Tool 스키마 필수 항목을 Tool Pod의 등록, Manifest, MCP 노출, Scaffold 및 빌드 검증 전 구간에 일관되게 적용한다.

**Architecture:** 각 Tool의 업무 명세는 `tool-definitions/{category}/{tool-name}.yml`에서 관리하고, 기동 시 공통 로더가 이를 읽어 기존 어노테이션 정보와 결합한다. 결합된 `ToolMetadata`를 단일 원천으로 Manifest와 MCP Tool을 생성해 Portal/Gateway/직접 MCP 연결 간 메타데이터 차이를 제거한다.

**Tech Stack:** Java 21, Spring Boot 3.5.11, Spring AI MCP 1.1.8, Jackson YAML/JSON, Gradle, JUnit 5, AssertJ

## Global Constraints

- Tool `name`은 `^[a-z][a-z0-9_]{2,63}$`를 만족한다.
- V17 필수 14개 항목을 누락 없이 제공한다.
- `example_queries`는 3~10개이며 Tool 이름을 직접 포함하지 않는다.
- `parameters_schema`는 루트 `type: object`, `additionalProperties: false`이고 모든 property에 description을 둔다.
- `outputSchema`를 명시한 Tool만 출력 검증하며 기존 우선순위를 유지한다.
- 기존 SSE/Streamable HTTP 전송, trace-id/request-id/employee-id, MCI/HTTP 호출 흐름은 변경하지 않는다.
- 기존 미추적 WCM/HMCI 파일을 삭제하거나 덮어쓰지 않는다.
- `register=false`인 Tool을 임의로 운영 등록 상태로 변경하지 않는다.

---

### Task 1: V17 Tool Definition 모델과 로더

**Files:**
- Create: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/metadata/ToolDescription.java`
- Create: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/metadata/ToolDefinition.java`
- Create: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/metadata/ToolDefinitionRepository.java`
- Create: `dat-was-lib/src/test/java/io/shinhanlife/dat/lib/metadata/ToolDefinitionRepositoryTest.java`
- Modify: `dat-was-lib/src/main/java/io/shinhanlife/dat/mcc/dto/ToolMetadata.java`

**Interfaces:**
- Produces: `Optional<ToolDefinition> findByName(String name)` 및 `ToolMetadata`의 V17 필드 접근자.
- Consumes: classpath `tool-definitions/**/*.yml`과 Jackson YAML.

- [ ] **Step 1: Write failing loader and validation tests**

  빈 필수 설명, 2개 이하 예시 질의, 잘못된 name, object가 아닌 schema를 거부하고 정상 YAML을 로드하는 테스트를 작성한다.

- [ ] **Step 2: Run tests and verify RED**

  Run: `./gradlew.bat :dat-was-lib:test --tests "*ToolDefinitionRepositoryTest"`
  Expected: FAIL because V17 model/repository does not exist.

- [ ] **Step 3: Implement minimal immutable models and classpath loader**

  `ToolDescription(function, whenToUse, whenNotToUse, ioLimits)`와 V17 전체 필드를 가진 `ToolDefinition`을 만들고 기동 시 한 번 로드·검증해 name 기준 불변 Map으로 캐시한다.

- [ ] **Step 4: Run tests and verify GREEN**

  Run: `./gradlew.bat :dat-was-lib:test --tests "*ToolDefinitionRepositoryTest"`
  Expected: PASS.

### Task 2: Registry 수집과 Manifest 표준 매핑

**Files:**
- Modify: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/mcp/ToolRegistryHeartbeatSender.java`
- Modify: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/manifest/ToolManifestItem.java`
- Modify: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/manifest/ToolManifestMeta.java`
- Modify: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/manifest/ToolManifestService.java`
- Modify: `dat-was-lib/src/test/java/io/shinhanlife/dat/lib/mcp/ToolRegistryHeartbeatSenderTest.java`
- Modify: `dat-was-lib/src/test/java/io/shinhanlife/dat/mcc/manifest/ToolManifestServiceTest.java`

**Interfaces:**
- Consumes: `ToolDefinitionRepository.findByName`.
- Produces: V17 description, display description, examples, owner/version/interface/environment metadata가 포함된 `ToolMetadata`와 `/manifest` 응답.

- [ ] **Step 1: Add failing enrichment and manifest mapping tests**
- [ ] **Step 2: Run targeted tests and verify RED**

  Run: `./gradlew.bat :dat-was-lib:test --tests "*ToolRegistryHeartbeatSenderTest" --tests "*ToolManifestServiceTest"`

- [ ] **Step 3: Merge annotation runtime data with definition YAML**

  호출 주소·상태는 런타임 값, 업무 설명·예시·소유조직은 YAML 값을 사용하며 필수 정의가 없는 등록 대상 Tool은 기동 검증에서 실패시킨다.

- [ ] **Step 4: Map Manifest title/description/inputSchema/outputSchema/annotations/_meta**
- [ ] **Step 5: Run targeted tests and verify GREEN**

### Task 3: MCP Tool 직접 노출과 Gateway 동기화

**Files:**
- Modify: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/mcp/ToolPodMcpToolSynchronizer.java`
- Create or Modify: `dat-was-lib/src/test/java/io/shinhanlife/dat/lib/mcp/ToolPodMcpToolSynchronizerTest.java`
- Modify: `dat-gateway/src/main/java/io/shinhanlife/dat/mcg/sync/RegistryMcpToolSpecificationFactory.java`
- Modify: `dat-gateway/src/test/java/io/shinhanlife/dat/biz/mcp/gateway/sync/RegistryMcpToolSpecificationFactoryTest.java`

**Interfaces:**
- Consumes: enriched `ToolMetadata`.
- Produces: 동일한 MCP `name`, `title`, 합성 description, input/output schema, readOnly/destructive/idempotent annotations, `_meta`.

- [ ] **Step 1: Add failing parity tests for Pod and Gateway MCP specs**
- [ ] **Step 2: Run both module tests and verify RED**
- [ ] **Step 3: Implement shared metadata-to-MCP mapping without changing transport**
- [ ] **Step 4: Run both module tests and verify GREEN**

### Task 4: 기존 Tool 전체 정의와 이름 정규화

**Files:**
- Create: `dat-was-sms/src/main/resources/tool-definitions/**/*.yml`
- Create: `dat-was-oth/src/main/resources/tool-definitions/**/*.yml`
- Modify: all `dat-was-sms/src/main/java/**/*UseCase*.java` containing `@McpTool`
- Modify: all `dat-was-oth/src/main/java/**/*UseCase*.java` containing `@McpTool`
- Modify: matching tests and mock fixture keys

**Interfaces:**
- Consumes: Task 1 YAML format.
- Produces: every discovered Tool has one unique V17 definition.

- [ ] **Step 1: Add failing repository-wide compliance test**

  모든 `@McpTool` name에 정확히 하나의 정의가 있고 필수 필드/예시 수/name 규칙을 만족하는지 검사한다.

- [ ] **Step 2: Run compliance test and verify RED**
- [ ] **Step 3: Rename invalid names and add complete definitions**

  `smp_exchangeRate_inquiry`는 `smp_exchange_inquiry`, `cmm_commonCode_lookup`는 `cmm_comcode_lookup`로 바꾸고 나머지 호출명은 호왘성을 위해 유지한다.

- [ ] **Step 4: Update tests/fixtures and verify GREEN**

### Task 5: Scaffold V17 생성 지원

**Files:**
- Modify: `dat-gateway/src/main/resources/static/admin/scaffold.html`
- Modify: Scaffold request DTO/controller files discovered under `dat-gateway/src/main/java`
- Modify: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java`
- Modify: `dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/ToolScaffolderTest.java`

**Interfaces:**
- Consumes: 입력한 function/when-to-use/when-not-to-use/io-limits/display-description/examples/owner/tags/hints.
- Produces: Java 소스, schemas, V17 tool-definition YAML.

- [ ] **Step 1: Add failing scaffold generation tests**
- [ ] **Step 2: Verify RED**
- [ ] **Step 3: Add form fields, validation, preview and YAML generation**
- [ ] **Step 4: Verify GREEN including duplicate DTO-field regression tests**

### Task 6: Gradle 품질 게이트와 문서

**Files:**
- Create: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/validation/ToolSchemaV17ValidationRunner.java`
- Create: `dat-was-lib/src/test/java/io/shinhanlife/dat/lib/validation/ToolSchemaV17ValidationRunnerTest.java`
- Modify: `build.gradle`
- Modify: `README.md`

**Interfaces:**
- Produces: `validateToolSchemaV17` Gradle task; `bootJar` before validation; file/Tool/field가 표시되는 실패 메시지.

- [ ] **Step 1: Add failing validation runner tests**
- [ ] **Step 2: Verify RED**
- [ ] **Step 3: Implement runner and wire `bootJar.dependsOn(validateToolSchemaV17)`**
- [ ] **Step 4: Document V17 fields, examples, Scaffold, compatibility and commands**
- [ ] **Step 5: Run targeted and full verification**

  Run:
  - `./gradlew.bat :dat-was-lib:test`
  - `./gradlew.bat :dat-was-sms:test :dat-was-oth:test :dat-gateway:test`
  - `./gradlew.bat validateMcpToolNames validateToolSchemaV17`
  - `./gradlew.bat clean build`

  Expected: all tasks succeed with zero test failures.
