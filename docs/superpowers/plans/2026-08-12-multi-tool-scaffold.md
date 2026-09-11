# Multi-tool Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate one UseCase containing multiple MCP Tool methods, each backed by its own typed Client, with enum and list DTO fields.

**Architecture:** Existing `scaffold(...)` overloads stay compatible. A new grouped request model generates one UseCase interface and implementation, while writing DTOs, Clients, IOs, converters, mock responses, and V17 metadata for each Tool.

**Tech Stack:** Java 21, Spring Boot, Spring AI MCP annotations, MapStruct, Jackson, JUnit 5, Bootstrap static HTML.

## Global Constraints

- Existing single-Tool Scaffold requests and `ToolScaffolder.scaffold(...)` overloads retain their behavior.
- Only HTTP and MCI routing are supported.
- Each MCI Tool calls a typed `*Client` method; only the Client calls `AxhubMciComponent`.
- Every generated Tool has a unique MCP name, method name, DTO pair, mock response, and V17 YAML definition.
- Generated DTOs use `@Schema`, never `McpToolParam`.
- Enum and list types match across Java DTOs, mock JSON, and V17 `parameters_schema`.

---

### Task 1: Define grouped Tool and structured-field models

**Files:**
- Modify: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java:43-61`
- Modify: `dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/ToolScaffolderTest.java`

**Interfaces:**
- Produces `FieldDefinition(name, type, description, example, required, enumValues, itemType, itemFields)`.
- Produces `ToolMethodDefinition(baseName, methodName, interfaceId, title, description, group, routingType, register, clientSystemCode, httpApiName, inputFields, outputFields, definitionOptions)`.
- Produces `scaffoldUseCase(String useCaseName, String moduleName, String author, String createDate, List<ToolMethodDefinition> tools)`.

- [ ] **Step 1: Write the failing grouped-generator test**

```java
ToolScaffolder.scaffoldUseCase("Customer", moduleName, "tester", "2026.08.12", List.of(
    mciTool("CustomerGuidance", "searchGuidance", "CTMNILO00007", "nilD"),
    mciTool("CustomerContract", "searchContract", "CTMCNT00001", "cntD")));
assertTrue(useCase.contains("CustomerGuidanceResponse searchGuidance(CustomerGuidanceRequest req)"));
assertTrue(implementation.contains("private final CustomerGuidanceClient customerGuidanceClient;"));
```

- [ ] **Step 2: Run the focused test and confirm it fails**

Run: `./gradlew.bat :dat-was-lib:test --tests "*ToolScaffolderTest.generatesOneUseCaseWithTwoMcpToolMethodsAndDistinctClients"`

Expected: FAIL because `scaffoldUseCase` is absent.

- [ ] **Step 3: Add the records and deterministic validation**

```java
public record ToolMethodDefinition(String baseName, String methodName, String interfaceId,
        String title, String description, String group, String routingType, boolean register,
        String clientSystemCode, String httpApiName, List<FieldDefinition> inputFields,
        List<FieldDefinition> outputFields, ToolDefinitionOptions definitionOptions) { }
```

Reject blank names and duplicate MCP Tool or Java method names before writing files.

- [ ] **Step 4: Run the focused test again and commit**

Run: `./gradlew.bat :dat-was-lib:test --tests "*ToolScaffolderTest.generatesOneUseCaseWithTwoMcpToolMethodsAndDistinctClients"`

Expected: still FAIL only because source-generation code is absent.

```bash
git add dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/ToolScaffolderTest.java
git commit -m "feat: define grouped tool scaffold model"
```

### Task 2: Generate multi-Tool UseCase and typed Client integration

**Files:**
- Modify: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java:145-1110`
- Modify: `dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/ToolScaffolderTest.java`

**Interfaces:**
- Consumes `ToolMethodDefinition` and `FieldDefinition` from Task 1.
- Produces one `<UseCaseName>UseCase`, one `<UseCaseName>UseCaseImpl`, and one typed Client per Tool.

- [ ] **Step 1: Extend the test for the CustomerGuidance flow**

```java
assertTrue(implementation.contains("CustomerGuidance_I request = converter.toCustomerGuidanceRequest(req);"));
assertTrue(implementation.contains("CustomerGuidance_O response = customerGuidanceClient.callCustomerGuidance(request);"));
assertTrue(implementation.contains("return converter.toCustomerGuidanceResponse(response);"));
```

- [ ] **Step 2: Run the test and confirm it fails**

Run: `./gradlew.bat :dat-was-lib:test --tests "*ToolScaffolderTest.generatesOneUseCaseWithTwoMcpToolMethodsAndDistinctClients"`

Expected: FAIL because generated source has only `execute(...)`.

- [ ] **Step 3: Implement `scaffoldUseCase(...)` source writers**

```java
public static String scaffoldUseCase(String useCaseName, String moduleName, String author,
        String createDate, List<ToolMethodDefinition> tools) throws IOException {
    // Write one interface and implementation; append one annotated method per Tool.
    // Write Tool-specific DTO, IO, converter, Client, mock, and YAML through shared helpers.
}
```

Generate an MCI Client method per Tool:

```java
public CustomerGuidance_O callCustomerGuidance(CustomerGuidance_I request) {
    return mci.callTo("CTMNILO00007", null, request, CustomerGuidance_O.class).getBody();
}
```

Each implementation method must use `request -> converter -> Client -> converter -> response`, and only set `resultCode` to `SUCCESS` after mapping a response.

- [ ] **Step 4: Generate V17 YAML and mock JSON separately for each Tool**

```java
for (ToolMethodDefinition tool : tools) {
    writeUtf8(definitionDir.resolve(toToolName(...) + ".yml"), toolDefinitionContentV17(...));
}
```

- [ ] **Step 5: Run all generator tests and commit**

Run: `./gradlew.bat :dat-was-lib:test --tests "*ToolScaffolderTest"`

Expected: PASS.

```bash
git add dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/ToolScaffolderTest.java
git commit -m "feat: generate multi-tool usecases with typed clients"
```

### Task 3: Generate enum and list DTO/schema shapes

**Files:**
- Modify: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java:1144-1420`
- Modify: `dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/ToolScaffolderTest.java`

**Interfaces:**
- Consumes structured `FieldDefinition` from Task 1.
- Produces named enum source, primitive `List<T>`, object-list `List<...Item>`, JSON-safe mock output, and matching V17 schema.

- [ ] **Step 1: Write failing structured-field tests**

```java
assertTrue(request.contains("private ClaimStatus claimStatus;"));
assertTrue(Files.exists(dtoRoot.resolve("ClaimStatus.java")));
assertTrue(request.contains("private List<String> customerIds;"));
assertTrue(response.contains("private List<GuidanceItem> guidanceItems;"));
assertTrue(Files.readString(definition).contains("type: array"));
```

- [ ] **Step 2: Run the test and confirm it fails**

Run: `./gradlew.bat :dat-was-lib:test --tests "*ToolScaffolderTest.generatesEnumAndListFields"`

Expected: FAIL because `Enum` and `List` are unsupported types.

- [ ] **Step 3: Generate the Java types**

```java
private static String javaFieldType(FieldDefinition field) {
    return switch (field.type()) {
        case "Enum" -> toPascalCase(field.name());
        case "List" -> "List<" + listItemJavaType(field) + ">";
        default -> supportedScalarType(field.type());
    };
}
```

Create enum values as valid Java constants with `@JsonValue`. Generate `<OwnerDto><FieldName>Item` for an object list and add MapStruct list mapping methods when Tool and IO item types differ.

- [ ] **Step 4: Generate schema and mock JSON**

```yaml
claimStatus:
  type: string
  enum: [OPEN, CLOSED]
customerIds:
  type: array
  items:
    type: string
```

For object lists, mocks contain a one-element object array using declared item examples.

- [ ] **Step 5: Run all generator tests and commit**

Run: `./gradlew.bat :dat-was-lib:test --tests "*ToolScaffolderTest"`

Expected: PASS.

```bash
git add dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/ToolScaffolderTest.java
git commit -m "feat: generate enum and list scaffold fields"
```

### Task 4: Expose grouped generation in the API and Scaffold UI

**Files:**
- Modify: `dat-gateway/src/main/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingController.java:77-260`
- Modify: `dat-gateway/src/test/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingControllerToolDraftTest.java`
- Modify: `dat-gateway/src/main/resources/static/admin/scaffold.html:880-1750`

**Interfaces:**
- Consumes `{ "useCaseName": "Customer", "moduleName": "dat-was-oth", "tools": [ ... ] }`.
- Produces `POST /api/v1/scaffold/tool-group` and keeps `POST /api/v1/scaffold/tool` unchanged.

- [ ] **Step 1: Write the failing controller test**

```java
mockMvc.perform(post("/api/v1/scaffold/tool-group")
        .contentType(APPLICATION_JSON).content(groupedRequestJson()))
    .andExpect(status().isOk())
    .andExpect(content().string(containsString("CustomerUseCase.java")));
```

- [ ] **Step 2: Run the controller test and confirm it fails**

Run: `./gradlew.bat :dat-gateway:test --tests "*ScaffoldingControllerToolDraftTest.groupedToolRequest"`

Expected: FAIL with 404 because `/tool-group` is absent.

- [ ] **Step 3: Add request parsing and validation**

```java
@PostMapping("/tool-group")
public String scaffoldToolGroup(@RequestBody ToolGroupRequest request) {
    validateUseCaseName(request.useCaseName());
    validateToolMethods(request.tools());
    return ToolScaffolder.scaffoldUseCase(request.useCaseName(), request.moduleName(),
        resolvedAuthor(request.author()), resolvedDate(request.date()), request.tools());
}
```

Reject duplicate Tool/method names, blank MCI interface IDs, invalid enum values, and List fields without either a primitive item type or object item fields.

- [ ] **Step 4: Update the Scaffold UI and AI draft support**

Add a UseCase name and an “Add Tool” card list. Each card owns its Tool name, method name, protocol, interface ID, Client system/API name, metadata, and I/O fields. Extend the field editor with `Enum` and `List`: Enum reveals allowed values; List reveals primitive/object item type and opens the item-field editor for objects. Serialize cards to `/tool-group`. Update AI prompts and validation to return those extra field properties; retain `/tool` and the existing one-Tool form.

- [ ] **Step 5: Run focused gateway and library tests, then commit**

Run: `./gradlew.bat :dat-gateway:test --tests "*ScaffoldingControllerToolDraftTest"; ./gradlew.bat :dat-was-lib:test --tests "*ToolScaffolderTest"`

Expected: PASS.

```bash
git add dat-gateway/src/main/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingController.java dat-gateway/src/test/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingControllerToolDraftTest.java dat-gateway/src/main/resources/static/admin/scaffold.html
git commit -m "feat: add grouped tool scaffold UI"
```

## Self-review

- Spec coverage: Tasks 1–2 provide multiple annotated methods and per-Tool Clients; Task 3 provides enum/List DTO and schema output; Task 4 provides the API and editor while preserving single-Tool compatibility.
- Placeholder scan: every task includes target files, verification commands, and concrete source shape.
- Type consistency: `FieldDefinition`, `ToolMethodDefinition`, and `scaffoldUseCase(...)` are defined in Task 1 and used with identical names later.
