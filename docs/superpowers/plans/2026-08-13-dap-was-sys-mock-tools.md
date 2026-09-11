# dat-was-sys Mock Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add two read-only system mock tools to `dat-was-sys`, with MCP YAML definitions and deterministic mock JSON responses.

**Architecture:** YAML definitions under `tool-definitions/sys` describe the tools. Their paired `mock-responses` files provide synthetic data. A resource-based JUnit test verifies the tool names, immutable safety flags, and valid response shape.

**Tech Stack:** Spring Boot 3, JUnit 5, Jackson, YAML.

## Global Constraints

- Create tool resources only in `dat-was-sys`.
- Both tools must be `read_only: true`, `destructive: false`, and `idempotent: true`.
- Do not include personal, customer, credential, or secret data.
- Follow the field layout in `dat-was-cus/src/main/resources/tool-definitions/smp/smp_team_list.yml`.

---

### Task 1: System status mock tool

**Files:**

- Create: `dat-was-sys/src/main/resources/tool-definitions/sys/sys_system_status.yml`
- Create: `dat-was-sys/src/main/resources/mock-responses/sys_system_status.json`
- Create: `dat-was-sys/src/test/java/io/shinhanlife/dat/mcc/sys/SystemMockToolResourcesTest.java`

**Interfaces:** Accepts optional `environment: string`; returns mock `systemName`, `environment`, `status`, and `checkedAt` values.

- [ ] Write a JUnit test that loads the two classpath resources; asserts tool name `sys_system_status`, `read_only: true`, `destructive: false`, optional `environment`, `resultCode` `SUCCESS`, and all four response fields.
- [ ] Run `./gradlew.bat :dat-was-sys:test --tests io.shinhanlife.dat.mcc.sys.SystemMockToolResourcesTest.systemStatusDefinitionIsReadOnlyAndHasValidMockResponse`; expect failure because both resources are absent.
- [ ] Create the definition with `category_key: sys`, no required environment keys, `additionalProperties: false`, and read-only safety fields. Create a `SUCCESS` mock JSON response whose stringified `data` contains only synthetic status data.
- [ ] Re-run the focused test; expect pass.
- [ ] Commit the task: `git add dat-was-sys/src/main/resources/tool-definitions/sys/sys_system_status.yml dat-was-sys/src/main/resources/mock-responses/sys_system_status.json dat-was-sys/src/test/java/io/shinhanlife/dat/mcc/sys/SystemMockToolResourcesTest.java && git commit -m "feat: add system status mock tool"`.

### Task 2: System notices mock tool

**Files:**

- Create: `dat-was-sys/src/main/resources/tool-definitions/sys/sys_notice_list.yml`
- Create: `dat-was-sys/src/main/resources/mock-responses/sys_notice_list.json`
- Modify: `dat-was-sys/src/test/java/io/shinhanlife/dat/mcc/sys/SystemMockToolResourcesTest.java`

**Interfaces:** Accepts optional `category: string`; returns a mock array containing notices with `title`, `priority`, and `publishedDate`.

- [ ] Add a JUnit test that loads both resources; asserts tool name `sys_notice_list`, `read_only: true`, `destructive: false`, optional `category`, `resultCode` `SUCCESS`, and at least one mock notice with all three fields.
- [ ] Run `./gradlew.bat :dat-was-sys:test --tests io.shinhanlife.dat.mcc.sys.SystemMockToolResourcesTest.noticeListDefinitionIsReadOnlyAndHasValidMockResponse`; expect failure because both resources are absent.
- [ ] Create the definition with `category_key: sys`, no required environment keys, `additionalProperties: false`, and read-only safety fields. Create a `SUCCESS` mock JSON response containing only synthetic system notices.
- [ ] Run `./gradlew.bat :dat-was-sys:test`; expect pass.
- [ ] Commit the task: `git add dat-was-sys/src/main/resources/tool-definitions/sys/sys_notice_list.yml dat-was-sys/src/main/resources/mock-responses/sys_notice_list.json dat-was-sys/src/test/java/io/shinhanlife/dat/mcc/sys/SystemMockToolResourcesTest.java && git commit -m "feat: add system notice mock tool"`.

## Plan Self-Review

- Spec coverage: Tasks 1 and 2 each add a tool definition and a paired mock response; both enforce read-only behavior. Tests validate resource availability and structure.
- Placeholder scan: No incomplete tasks or unspecified file paths remain.
- Type consistency: Both responses use `resultCode` plus stringified JSON `data`, matching the existing mock-response convention.
