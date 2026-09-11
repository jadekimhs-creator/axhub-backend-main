# Scaffold AI V17 Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AI Tool 초안 생성 시 Tool Schema V17 Metadata 전체를 생성하고 Scaffold 폼에 자동 입력한다.

**Architecture:** 기존 `/tool-draft` 응답 DTO를 확장하고 서버에서 값을 정규화한다. 화면은 서버 응답의 필드를 기존 V17 폼 요소에 직접 매핑하며, 실제 Scaffold 생성은 기존 `ToolDefinitionOptions` 경로를 그대로 사용한다.

**Tech Stack:** Java 21, Spring Boot MVC, Jackson, JUnit 5, MockMvc, HTML/JavaScript

## Global Constraints

- 기존 Tool 생성 경로와 Legacy 연동 정보 입력 정책을 유지한다.
- Tool Schema V17 필드명은 기존 `ToolDefinitionOptions`와 동일하게 유지한다.
- 기존 작업 트리 변경사항을 되돌리거나 포함 범위 밖에서 수정하지 않는다.

---

### Task 1: AI Tool Draft 서버 계약 확장

**Files:**
- Modify: `dat-gateway/src/test/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingControllerToolDraftTest.java`
- Modify: `dat-gateway/src/main/java/io/shinhanlife/dat/mcg/presentation/ScaffoldingController.java`

**Interfaces:**
- Consumes: AI가 반환한 JSON Tool 초안
- Produces: V17 Metadata가 포함된 `/api/v1/scaffold/tool-draft` JSON 응답

- [ ] Mock AI 응답과 API assertion에 V17 필드를 추가한다.
- [ ] 테스트를 실행해 현재 응답에서 V17 필드가 누락되어 실패하는지 확인한다.
- [ ] `ToolDraft`와 프롬프트 및 정규화 로직을 확장한다.
- [ ] 테스트가 통과하는지 확인한다.

### Task 2: Scaffold 화면 자동 매핑

**Files:**
- Modify: `dat-gateway/src/main/resources/static/admin/scaffold.html`

**Interfaces:**
- Consumes: Task 1의 V17 Metadata JSON 필드
- Produces: 같은 이름을 가진 V17 폼 입력값

- [ ] `createAiToolDraft()`에 문자열 필드 매핑을 추가한다.
- [ ] `exampleQueries`는 줄바꿈, `tags`는 쉼표 구분 문자열로 변환한다.
- [ ] 누락된 값은 빈 값 또는 `MCP_TOOL` 기본값으로 처리한다.

### Task 3: 통합 검증

**Files:**
- Verify: Gateway와 V17 관련 전체 변경

**Interfaces:**
- Consumes: Task 1~2 결과
- Produces: 컴파일 및 표준 검증 증거

- [ ] Gateway 대상 테스트를 실행한다.
- [ ] `validateToolSchemaV17`을 실행한다.
- [ ] `git diff --check`로 문법적 공백 오류를 확인한다.
