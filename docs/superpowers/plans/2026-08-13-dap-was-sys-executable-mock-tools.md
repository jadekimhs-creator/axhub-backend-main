# dat-was-sys Executable Mock Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose the two existing system mock definitions as executable MCP tools and REST endpoints.

**Architecture:** Request DTOs enter `@McpTool` UseCase interfaces and REST controller methods. Spring services return deterministic response DTOs, with no external dependencies. The controller delegates to the UseCases so both interfaces share behavior.

**Tech Stack:** Spring Boot, Spring AI Community MCP annotations, Lombok, JUnit 5, MockMvc.

## Global Constraints

- Tools are read-only, non-destructive, idempotent, and synthetic-only.
- Endpoints are `GET /api/sys/status` and `GET /api/sys/notices`.
- Keep existing YAML and JSON mock resources aligned with response DTO fields.

---

### Task 1: Executable system-status tool

**Files:**

- Create: `dat-was-sys/src/main/java/io/shinhanlife/dat/mcc/sys/status/dto/SystemStatusRequest.java`
- Create: `dat-was-sys/src/main/java/io/shinhanlife/dat/mcc/sys/status/dto/SystemStatusResponse.java`
- Create: `dat-was-sys/src/main/java/io/shinhanlife/dat/mcc/sys/status/usecase/SystemStatusUseCase.java`
- Create: `dat-was-sys/src/main/java/io/shinhanlife/dat/mcc/sys/status/usecase/impl/SystemStatusUseCaseImpl.java`
- Test: `dat-was-sys/src/test/java/io/shinhanlife/dat/mcc/sys/status/usecase/impl/SystemStatusUseCaseImplTest.java`

- [ ] Write a test expecting default `development`, `AX Hub System`, `HEALTHY`, and a nonblank ISO timestamp; run it and observe compilation failure because the types do not exist.
- [ ] Implement request/response DTOs, MCP-annotated `getSystemStatus(SystemStatusRequest)`, and the service returning those literal mock values; rerun the focused test and expect pass.

### Task 2: Executable notice-list tool

**Files:**

- Create: `dat-was-sys/src/main/java/io/shinhanlife/dat/mcc/sys/notice/dto/SystemNoticeRequest.java`
- Create: `dat-was-sys/src/main/java/io/shinhanlife/dat/mcc/sys/notice/dto/SystemNoticeResponse.java`
- Create: `dat-was-sys/src/main/java/io/shinhanlife/dat/mcc/sys/notice/usecase/SystemNoticeUseCase.java`
- Create: `dat-was-sys/src/main/java/io/shinhanlife/dat/mcc/sys/notice/usecase/impl/SystemNoticeUseCaseImpl.java`
- Test: `dat-was-sys/src/test/java/io/shinhanlife/dat/mcc/sys/notice/usecase/impl/SystemNoticeUseCaseImplTest.java`

- [ ] Write a test expecting at least two notices with title, priority, and published date; run it and observe compilation failure because the types do not exist.
- [ ] Implement the DTOs, MCP-annotated `getSystemNotices(SystemNoticeRequest)`, and service returning deterministic notices; rerun the focused test and expect pass.

### Task 3: REST adapter

**Files:**

- Create: `dat-was-sys/src/main/java/io/shinhanlife/dat/mcc/sys/presentation/SystemMockToolController.java`
- Test: `dat-was-sys/src/test/java/io/shinhanlife/dat/mcc/sys/presentation/SystemMockToolControllerTest.java`

- [ ] Write MockMvc tests for both GET endpoints using optional query parameters and asserting response fields; run and observe compilation failure because the controller does not exist.
- [ ] Implement a controller that constructs request DTOs, delegates to the UseCases, and returns response DTOs; run `./gradlew.bat :dat-was-sys:test` and expect pass.

## Plan Self-Review

- The plan covers executable MCP registration, response DTOs, REST access, and tests for both tool contracts.
- Each runtime response remains synthetic and read-only.
