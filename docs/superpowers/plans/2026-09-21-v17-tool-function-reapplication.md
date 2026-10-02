# V17 Tool Function Reapplication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Make Tool Function AI optimization and generated V17 YAML consistently enforce the approved read-only, Ground Truth, naming, metadata, and paging rules.

**Architecture:** Add a shared V17 policy in dat-was-lib. Gateway uses it to normalize and validate AI results; ToolScaffolder uses it to generate YAML defaults. The browser identifies immutable Ground Truth and never lets AI output replace it.

**Tech Stack:** Java 21, Spring MVC, JUnit 5, MockMvc, static HTML/JavaScript, Gradle.

**Spec:** docs/superpowers/specs/2026-09-21-v17-tool-function-reapplication-design.md

## Global Constraints

- Generated V17 definitions use read_only: true, destructive: false, idempotent: true.
- Published MCP name is categoryKey_search-or-detail_target; Java Base Name excludes categoryKey.
- title and displayDescription are immutable during AI optimization.
- exampleQueries has 3 to 10 entries; tags has 5 to 8 entries and starts with categoryKey.
- pageInfo and scrPageInfo retain exact names and are optional system-owned fields.
- Preserve existing local edits outside plan files and hunks.

## Review Focus

- Normalize ProSearchFund and pro_search_fund to SearchFund, avoiding pro_pro_search_fund.
- Reject inquiry, query, and write verbs.
- Preserve submitted title and displayDescription despite changed AI output.
- Reject 2 example queries or 2 tags.
- Restore pageInfo and scrPageInfo names and optional status.

---

### Task 1: Shared V17 schema policy

**Files:**
- Create: dat-was-lib/src/main/java/io/shinhanlife/dat/lib/metadata/V17ToolSchemaPolicy.java
- Create: dat-was-lib/src/test/java/io/shinhanlife/dat/lib/metadata/V17ToolSchemaPolicyTest.java

**Interfaces:**
- Produces normalizeJavaBaseName(String rawBaseName, String categoryKey).
- Produces validateExamples(List<String>), validateTags(List<String>, String), and normalizePagingFields(List<FieldDefinition>).

- [ ] **Step 1: Write the failing tests**

    @Test
    void removesCategoryPrefixFromThreePartMcpName() {
        assertEquals("SearchFund",
                V17ToolSchemaPolicy.normalizeJavaBaseName("pro_search_fund", "pro"));
        assertEquals("DetailContract",
                V17ToolSchemaPolicy.normalizeJavaBaseName("ProDetailContract", "pro"));
    }

    @Test
    void rejectsUnsupportedActionAndInvalidMetadataCardinality() {
        assertThrows(IllegalArgumentException.class,
                () -> V17ToolSchemaPolicy.normalizeJavaBaseName("ProInquiryFund", "pro"));
        assertThrows(IllegalArgumentException.class,
                () -> V17ToolSchemaPolicy.validateExamples(List.of("하나", "둘")));
        assertThrows(IllegalArgumentException.class,
                () -> V17ToolSchemaPolicy.validateTags(List.of("pro", "펀드"), "pro"));
    }

- [ ] **Step 2: Run the focused test to verify it fails**

Run: ./gradlew.bat :dat-was-lib:test --tests io.shinhanlife.dat.lib.metadata.V17ToolSchemaPolicyTest --no-daemon

Expected: FAIL because V17ToolSchemaPolicy does not exist.

- [ ] **Step 3: Implement the policy**

    public static String normalizeJavaBaseName(String rawBaseName, String categoryKey) {
        List<String> parts = splitPascalOrDelimited(rawBaseName);
        if (parts.size() == 3 && parts.getFirst().equalsIgnoreCase(categoryKey)) {
            parts = parts.subList(1, parts.size());
        }
        if (parts.size() < 2 || !Set.of("search", "detail")
                .contains(parts.getFirst().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("V17 Tool 기능어는 search 또는 detail만 사용할 수 있습니다.");
        }
        return toPascalCase(parts);
    }

Implement exact 3~10 example and 5~8 tag checks. Require the first tag to equal categoryKey. Return pageInfo and scrPageInfo with exact names, required=false, and the existing paging description.

- [ ] **Step 4: Run the focused test to verify it passes**

Run: ./gradlew.bat :dat-was-lib:test --tests io.shinhanlife.dat.lib.metadata.V17ToolSchemaPolicyTest --no-daemon

Expected: PASS.

- [ ] **Step 5: Commit the policy**

    git add dat-was-lib/src/main/java/io/shinhanlife/dat/lib/metadata/V17ToolSchemaPolicy.java dat-was-lib/src/test/java/io/shinhanlife/dat/lib/metadata/V17ToolSchemaPolicyTest.java
    git commit -m "feat: add V17 tool schema policy"

### Task 2: Enforce policy in Gateway Tool Function optimizer

**Files:**
- Modify: dat-gateway/src/main/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingController.java
- Modify: dat-gateway/src/test/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingControllerToolDraftTest.java

**Interfaces:**
- Consumes V17ToolSchemaPolicy and existing ToolDraft.
- Produces an optimized ToolDraft with Java Base Name without category duplication, protected Ground Truth, validated tags/examples, and canonical optional paging fields.

- [ ] **Step 1: Extend the existing optimize endpoint test**

    .andExpect(jsonPath("$.baseName").value("SearchVariable"))
    .andExpect(jsonPath("$.title").value(originalTitle))
    .andExpect(jsonPath("$.displayDescription").value(originalDisplayDesc))
    .andExpect(jsonPath("$.inputFields[1].name").value("scrPageInfo"))
    .andExpect(jsonPath("$.inputFields[1].required").value(false));

    assertTrue(promptCaptor.getValue().contains("idempotent=true"));
    assertTrue(promptCaptor.getValue().contains("Java Base Name excludes categoryKey"));

Add negative validateToolDraft tests for ProInquiryFund, two example queries, and two tags.

- [ ] **Step 2: Run the focused test to verify it fails**

Run: ./gradlew.bat :dat-gateway:test --tests io.shinhanlife.dat.mcg.presentation.ScaffoldingControllerToolDraftTest --no-daemon

Expected: FAIL because optimizer returns ProSearchVariable and permits invalid metadata cardinality.

- [ ] **Step 3: Update both Tool Draft prompts and validation**

    String finalBaseName = V17ToolSchemaPolicy.normalizeJavaBaseName(draft.baseName(), effectiveCatKey);
    List<String> finalExamples = V17ToolSchemaPolicy.validateExamples(draft.exampleQueries());
    List<String> finalTags = V17ToolSchemaPolicy.validateTags(draft.tags(), effectiveCatKey);
    List<FieldDefinition> finalInputs = V17ToolSchemaPolicy.normalizePagingFields(
            validateFields(draft.inputFields(), true));

Both prompts request SearchFund or DetailContract as Java Base Name; categoryKey is added later for the published Tool name. Both prompts state destructive=false and idempotent=true. Preserve submitted nonblank title/displayDescription after AI parsing.

- [ ] **Step 4: Run the focused test to verify it passes**

Run: ./gradlew.bat :dat-gateway:test --tests io.shinhanlife.dat.mcg.presentation.ScaffoldingControllerToolDraftTest --no-daemon

Expected: PASS.

- [ ] **Step 5: Commit the Gateway optimizer**

    git add dat-gateway/src/main/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingController.java dat-gateway/src/test/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingControllerToolDraftTest.java
    git commit -m "feat: enforce V17 tool optimization rules"

### Task 3: Generate compliant V17 YAML fallback metadata

**Files:**
- Modify: dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java
- Modify: dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/ToolScaffolderTest.java

**Interfaces:**
- Consumes V17ToolSchemaPolicy, ToolDefinitionOptions, FieldDefinition, and generated Tool name.
- Produces valid YAML with true/false/true safety flags, 5~8 tags, 3~10 examples, and optional canonical paging fields.

- [ ] **Step 1: Write failing generated-YAML assertions**

    assertTrue(yaml.contains("read_only: true"), yaml);
    assertTrue(yaml.contains("destructive: false"), yaml);
    assertTrue(yaml.contains("idempotent: true"), yaml);
    assertTrue(yaml.contains("tags: [smp,"), yaml);
    assertEquals(3, parsed.exampleQueries().size());
    assertTrue(parsed.tags().size() >= 5 && parsed.tags().size() <= 8);

Add a test passing scrPageInfo as required=true and assert generated YAML makes it optional without renaming it.

- [ ] **Step 2: Run focused tests to verify they fail**

Run: ./gradlew.bat :dat-was-lib:test --tests io.shinhanlife.dat.lib.util.ToolScaffolderTest.generatesV17ToolDefinitionTogetherWithToolSources --tests io.shinhanlife.dat.lib.util.ToolScaffolderTest.appliesV17MetadataEnteredByScaffoldUser --no-daemon

Expected: FAIL because fallback tags have only category and flags derive from mutation.

- [ ] **Step 3: Implement compliant defaults**

    List<String> tags = normalizedList(options.tags(), List.of(
            categoryKey.toLowerCase(Locale.ROOT), "업무조회", "상세확인", "search", "detail"));
    List<String> examples = normalizedList(options.exampleQueries(), List.of(
            displayDescription + " 조회해줘",
            displayDescription + " 정보를 알려줘",
            displayDescription + " 상세를 확인해줘"));

Run both lists through V17ToolSchemaPolicy before YAML output. Emit constant read_only true, destructive false, idempotent true; normalize paging fields before properties and required blocks.

- [ ] **Step 4: Run focused tests to verify they pass**

Run: ./gradlew.bat :dat-was-lib:test --tests io.shinhanlife.dat.lib.util.ToolScaffolderTest.generatesV17ToolDefinitionTogetherWithToolSources --tests io.shinhanlife.dat.lib.util.ToolScaffolderTest.appliesV17MetadataEnteredByScaffoldUser --no-daemon

Expected: PASS.

- [ ] **Step 5: Commit the generator**

    git add dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/ToolScaffolderTest.java
    git commit -m "feat: generate compliant read-only V17 tool definitions"

### Task 4: Clarify metadata ownership in Tool Function UI

**Files:**
- Modify: dat-gateway/src/main/resources/static/admin/scaffold.html
- Modify: dat-gateway/src/test/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingControllerToolDraftTest.java

**Interfaces:**
- Consumes existing form field names and optimize endpoint response.
- Produces unchanged payload shape, Korean Ground Truth help, and client-side preservation of nonblank title/displayDescription.

- [ ] **Step 1: Write failing static-page assertions**

    assertTrue(page.contains("Ground Truth (AI 변경 불가)"));
    assertTrue(page.contains("업무단위_search|detail_대상"));
    assertTrue(page.contains("태그 5~8개"));
    assertTrue(page.contains("조회 전용: destructive=false, idempotent=true"));

- [ ] **Step 2: Run test to verify it fails**

Run: ./gradlew.bat :dat-gateway:test --tests io.shinhanlife.dat.mcg.presentation.ScaffoldingControllerToolDraftTest.scaffoldPageContainsV17OptimizationButtonsAndScript --no-daemon

Expected: FAIL because ownership guidance is absent.

- [ ] **Step 3: Update the UI**

    const originalTitle = form.elements.title.value.trim();
    const originalDisplayDescription = form.elements.displayDescription.value.trim();
    if (originalTitle) form.elements.title.value = originalTitle;
    if (originalDisplayDescription) form.elements.displayDescription.value = originalDisplayDescription;

Add Korean guidance: published name follows three tiers; title/displayDescription are Ground Truth; tags have 5~8 entries; examples have 3~10 screen-based questions; generated Tools are read-only. Keep all field names and payload keys unchanged.

- [ ] **Step 4: Run test to verify it passes**

Run: ./gradlew.bat :dat-gateway:test --tests io.shinhanlife.dat.mcg.presentation.ScaffoldingControllerToolDraftTest.scaffoldPageContainsV17OptimizationButtonsAndScript --no-daemon

Expected: PASS.

- [ ] **Step 5: Commit the UI**

    git add dat-gateway/src/main/resources/static/admin/scaffold.html dat-gateway/src/test/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingControllerToolDraftTest.java
    git commit -m "feat: clarify V17 tool metadata ownership"

### Task 5: End-to-end V17 regression verification

**Files:**
- Modify: none

**Interfaces:**
- Consumes Tasks 1 through 4.
- Produces evidence that shared policy, Gateway optimization, YAML generation, and UI guidance agree.

- [ ] **Step 1: Run relevant tests**

Run: ./gradlew.bat :dat-was-lib:test --tests io.shinhanlife.dat.lib.metadata.V17ToolSchemaPolicyTest --tests io.shinhanlife.dat.lib.util.ToolScaffolderTest --no-daemon

Expected: PASS.

Run: ./gradlew.bat :dat-gateway:test --tests io.shinhanlife.dat.mcg.presentation.ScaffoldingControllerToolDraftTest --no-daemon

Expected: PASS.

- [ ] **Step 2: Compile and check whitespace**

Run: ./gradlew.bat :dat-gateway:compileJava --no-daemon

Expected: BUILD SUCCESSFUL.

Run: git diff --check

Expected: no whitespace errors.

- [ ] **Step 3: Inspect scoped delivery**

    git status --short
    git diff --cached --name-only

Expected: only Task 1–4 files are staged in their commits; application.yml, prior paging edits, and existing plan files remain untouched.

