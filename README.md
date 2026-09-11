# AX HUB MCP Tool Platform

## Current Tool Pod modules

| Pod role | Gradle module | Container service | Internal endpoint | Local Docker endpoint |
|---|---|---|---|---|
| Customer/common integration | `dat-was-cus` | `was-cus` | `http://was-cus:8084/mcp` | `http://localhost:8284/mcp` |
| Sales/notification integration | `dat-was-sal` | `was-sal` | `http://was-sal:8082/mcp` | `http://localhost:8282/mcp` |
| Process integration (new) | `dat-was-pro` | `was-pro` | `http://was-pro:8085/mcp` | `http://localhost:8085/mcp` |
| System integration (new) | `dat-was-sys` | `was-sys` | `http://was-sys:8086/mcp` | `http://localhost:8086/mcp` |

- The former `dat-was-oth` module is now `dat-was-cus`; the former `dat-was-sms` module is now `dat-was-sal`.
- This is a deployment Pod rename only. Existing tool category and function names such as `oth_*` and `sms_*` remain valid so already registered MCP clients are not broken.
- `dat-was-pro` and `dat-was-sys` are empty, independently deployable Tool Pods ready for new business tools.

### Run the Tool Pods locally

```powershell
.\gradlew.bat :dat-was-cus:bootRun
.\gradlew.bat :dat-was-sal:bootRun
.\gradlew.bat :dat-was-pro:bootRun
.\gradlew.bat :dat-was-sys:bootRun
```

AX HUB에서 AI Agent가 업무 Tool을 검색하고 호출할 수 있도록 Gateway와 독립 Tool Pod를 제공하는 멀티 모듈 Spring Boot 프로젝트입니다.

## 1. 현재 구성

```text
MCP Client / AI Agent
        |
        v
Gateway (dat-gateway, 8081)
  - MCP 연결, Tool Registry, 인증·인가, 라우팅, 공통 화면
        |
        +-------------------------+
        |                         |
        v                         v
Sales Tool Pod (dat-was-sal, 8082) Customer Tool Pod (dat-was-cus, 8084)
  - 영업/알림 업무 Tool             - 고객/공통 업무 Tool
  - Tool Manifest                  - Tool Manifest
  - Pod Test Console               - Pod Test Console
        |
        +--> Process Tool Pod (dat-was-pro, 8085)
        +--> System Tool Pod (dat-was-sys, 8086)
        |
        v
MCI / EAI / EIMS 등 레거시 연계 대상
```

Gateway는 외부 MCP 진입점과 Tool 등록·라우팅을 담당하고, 실제 업무 로직과 레거시 호출은 각 Tool Pod가 담당합니다. Tool Pod는 업무/조직 단위로 독립 배포할 수 있습니다.

## 2. 모듈

| 모듈 | 역할 | 기본 포트 |
|---|---|---:|
| `dat-gateway` | MCP Gateway, Registry, 라우팅, Chat·Catalog·Tester·Scaffold 화면 | 8081 |
| `dat-was-lib` | Tool 공통 라이브러리: 어노테이션, Schema, Manifest, MCI/EAI 어댑터, 공통 Web·보안 기능 | - |
| `dat-was-sal` | 영업/알림 업무 Tool Pod | 8082 |
| `dat-was-cus` | 고객/공통 업무 Tool Pod | 8084 |
| `dat-was-pro` | 프로세스 업무 Tool Pod | 8085 |
| `dat-was-sys` | 시스템 업무 Tool Pod | 8086 |

> 기존 `dap-tool-core`, `dap-tool-sms`, `dap-tool-oth` 명칭은 현재 각각 `dat-was-lib`, `dat-was-sal`, `dat-was-cus`로 변경되었습니다.

## 3. 개발 환경

| 항목 | 기준 |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.11 |
| Gradle | 8.14.3 (Wrapper) |
| Spring AI BOM | 1.1.8 |
| 기본 프로필 | `local` |

로컬 환경에서는 민감한 값은 환경 변수로만 주입합니다.

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:OPENROUTER_API_KEY = '<발급받은-키>'
```

## 4. 실행

### 4.1 Gradle 로컬 실행

각 애플리케이션은 별도 터미널에서 실행합니다.

```powershell
.\gradlew.bat :dat-gateway:bootRun
.\gradlew.bat :dat-was-sal:bootRun
.\gradlew.bat :dat-was-cus:bootRun
.\gradlew.bat :dat-was-pro:bootRun
.\gradlew.bat :dat-was-sys:bootRun
```

### 4.2 Docker Compose 실행

```powershell
docker compose up -d --build
```

| 서비스 | 호스트 URL | 컨테이너 포트 |
|---|---|---:|
| Gateway | http://localhost:8281 | 8081 |
| Sales Tool Pod | http://localhost:8282 | 8082 |
| Customer Tool Pod | http://localhost:8284 | 8084 |
| Process Tool Pod | http://localhost:8285 | 8085 |
| System Tool Pod | http://localhost:8286 | 8086 |
| MCI Mock | http://localhost:8089 | 8080 |
| Dozzle 로그 화면 | http://localhost:8288 | 8080 |

## 5. 화면과 운영 도구

### Gateway 화면

| 화면 | 로컬 URL | 용도 |
|---|---|---|
| 메인 | http://localhost:8081/index.html | 플랫폼 진입 화면 |
| Catalog | http://localhost:8081/catalog.html | 등록 Tool 탐색 |
| Playground | http://localhost:8081/playground.html | 단건 Tool 호출 확인 |
| Chat | http://localhost:8081/chat.html | Agent 기반 대화형 호출 |
| Auto Tester | http://localhost:8081/tester.html | Gateway 기준 다중 Tool 스모크/회귀 테스트 |
| Scaffold | http://localhost:8081/admin/scaffold.html | Pod·Tool 소스 생성 지원 |

Docker Compose로 실행한 경우 Gateway 화면은 포트 `8281`을 사용합니다.

### Pod Module New: 독립 프로젝트 생성

Scaffold 화면의 `Pod Module New` 탭은 기존 Pod Module 생성 흐름과 별도로 동작합니다. 새 모듈명과 포트, workspace 경로를 입력하면 해당 workspace 바로 아래에 독립 Tool Pod 프로젝트를 생성합니다.

기본 workspace는 `C:\eGovFrameDev-4.3.1-64bit\workspace`이며, 예를 들어 모듈명으로 `dat-was-payment`를 입력하면 `C:\eGovFrameDev-4.3.1-64bit\workspace\dat-was-payment` 프로젝트가 만들어집니다. 모듈명은 `dat-was-`로 시작해야 하며, 이미 같은 폴더가 있으면 덮어쓰지 않고 생성 요청을 거절합니다.

생성된 프로젝트는 같은 workspace의 `dat-lib-datmt`를 Gradle composite build로 참조합니다. 생성 직후 프로젝트 폴더에서 다음 명령으로 컴파일할 수 있습니다.

```powershell
cd C:\eGovFrameDev-4.3.1-64bit\workspace\dat-was-payment
.\gradlew.bat compileJava
```

이 기능은 기존 AX HUB 멀티 모듈 Pod나 기존 `Pod Module` 탭의 구성·소스를 수정하지 않습니다.

### Tool Pod Test Console

공통 정적 화면인 `tool-test-console.html`은 Tool Pod의 `/tool-manifest`를 읽어, 해당 Pod에 등록된 Tool과 `inputSchema`를 기준으로 요청 JSON을 만들어 직접 실행합니다.

| 대상 Pod | 로컬 URL | Docker Compose URL |
|---|---|---|
| Customer | http://localhost:8084/tool-test-console.html | http://localhost:8284/tool-test-console.html |
| Sales | http://localhost:8082/tool-test-console.html | http://localhost:8282/tool-test-console.html |
| Process | http://localhost:8085/tool-test-console.html | http://localhost:8285/tool-test-console.html |
| System | http://localhost:8086/tool-test-console.html | http://localhost:8286/tool-test-console.html |

사용 방법은 다음과 같습니다.

1. 대상 Tool Pod의 Console에 접속합니다.
2. Tool을 선택하고 `Schema 샘플 채우기`로 요청 JSON을 생성합니다.
3. 업무에 맞는 값으로 보정한 뒤 실행합니다.
4. 재사용할 요청은 `현재 요청 저장`으로 브라우저 `localStorage`에 저장합니다.

### Gateway Auto Tester

`tester.html`은 Gateway에 등록된 Tool을 대상으로 다음 기능을 제공합니다.

- Tool 목록 자동 조회 및 Tool별 실행
- JSON Schema 기반 더미 요청 생성, 사용자 요청 JSON 저장
- 필터된 Tool 일괄 실행 및 실패 Tool 재실행
- 결과 차트, 실시간 실행 로그, CSV 내보내기, 호출 예제 복사
- Tool별 자동 Ping(3초 간격) 및 Stress Test(현재 50건 동시 요청)

> Auto Tester의 더미 요청 성공은 **통신·등록·기본 스키마 확인용 스모크 테스트**입니다. 실제 MCI/EAI 업무 규칙, 권한, 데이터 정합성은 보장하지 않습니다. Stress Test와 Auto Ping은 실제 Tool·레거시 호출을 유발할 수 있으므로 개발/테스트 환경 및 승인된 테스트 데이터에서만 사용합니다.

UI에서 사용하는 Tailwind CSS와 Chart.js는 `dat-gateway/src/main/resources/static/lib`에 포함되어 있어, 화면 라이브러리 로딩을 위해 외부 CDN 연결이 필요하지 않습니다.

## 6. 주요 HTTP API

### Gateway API

| 기능 | Method | 경로 |
|---|---|---|
| 등록 Tool 목록 | GET | `/mcp/api/v1/tools/list` |
| Gateway를 통한 Tool 호출 | POST | `/mcp/api/v1/tools/call` |
| Tool 문서 Markdown | GET | `/mcp/api/v1/tools/docs/markdown` |
| Tool 등록 | POST | `/mcp/api/v1/registry/register` |
| Tool 해제 | POST | `/mcp/api/v1/registry/deregister` |
| Heartbeat | POST | `/mcp/api/v1/registry/heartbeat` |

호출 본문은 JSON-RPC 형식을 사용합니다.

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "cmm_claim_search",
    "arguments": {
      "claimNo": "CLM2026070100120"
    }
  },
  "id": 1
}
```

### Tool Pod API

| 기능 | Method | 경로 |
|---|---|---|
| 현재 Pod의 Tool Manifest | GET | `/tool-manifest` |
| 현재 Pod의 로컬 Tool 목록 | GET | `/mcp/api/v1/tools/local` |
| 현재 Pod에 직접 Tool 호출 | POST | `/mcp/{toolName}` |

Agent나 외부 클라이언트의 표준 MCP 진입은 Gateway를 사용합니다. Pod 직접 호출은 개발·단위 테스트·Pod Console 용도로 사용합니다.

## 7. Tool 개발 규칙

### 이름

Tool 함수명은 아래 4단계 규칙을 사용합니다.

```text
pod_domain_service_action
예: cmm_claim_search
```

- `pod`: Tool Pod 식별자 (`oth`, `sms` 등)
- `domain`: 업무 도메인 또는 `categoryKey` (`cmm`, `smp` 등)
- `service`: 업무 서비스명
- `action`: 동작 (`search`, `issue`, `send` 등)

### 기본 구현 계층

```text
Request DTO
  -> UseCase Interface
  -> UseCaseImpl
  -> Converter (AI DTO <-> MCI/EAI Interface IO)
  -> Client / Adapter
  -> Legacy System
```

- Request DTO 필드에는 `@Schema(description = "...")`와 Bean Validation 어노테이션을 선언합니다.
- 복잡하거나 조건부 규칙이 필요한 입력은 `tool-schemas/{categoryKey}/...-input-schema.json` 리소스를 사용합니다.
- 응답 Schema가 필요한 경우 Response DTO에 `@McpOutputSchema`를 선언하고, 복잡한 경우 Output Schema 리소스로 명시합니다.
- `null`의 업무 의미, 조건부 필드, 배열 정렬 기준, `hasMore` 여부, 민감 정보 제외 원칙을 Schema 설명에 명확히 씁니다.

### Schema 우선순위

1. `inputSchemaResource` 또는 `outputSchemaResource`가 지정된 경우: 해당 JSON Schema를 사용합니다.
2. 리소스가 없으면: DTO의 `@Schema` 정보와 공통 Generator로 생성합니다. 응답 Schema는 `@McpOutputSchema`가 선언된 DTO만 자동 생성합니다.

## 8. Manifest와 Registry

각 Tool Pod는 기동 시 `@McpTool`, `@McpFunction` 정보를 읽어 `/tool-manifest`를 제공합니다. Manifest에는 Tool 이름, 설명, endpoint, revision, Input/Output Schema가 포함됩니다.

Gateway는 Registry 등록 및 Heartbeat 정보를 이용해 Tool을 라우팅합니다. Tool Pod가 추가되어도 동일한 Manifest/Registry 규약을 준수하면 Gateway에서 탐색·호출할 수 있습니다.

## 9. MCI/EAI 연계

공통 MCI/EAI 연계 기능과 Glow 기본 설정은 `dat-was-lib`에서 제공합니다. Tool Pod별 애플리케이션 프로필은 각 Pod의 `application*.yml`에서 관리합니다.

```text
dat-was-lib/src/main/resources/glow/application-glow*.yml
dat-was-sal/src/main/resources/application*.yml
dat-was-cus/src/main/resources/application*.yml
dat-was-pro/src/main/resources/application*.yml
dat-was-sys/src/main/resources/application*.yml
```

MCI 호출 주소는 일반적으로 `host + uri`로 구성합니다. 예를 들어 `host=https://dev-ichmci.shinhanlife.co.kr`, `uri=/ntl_mci/clc_rcv`이면 호출 대상은 다음과 같습니다.

```text
https://dev-ichmci.shinhanlife.co.kr/ntl_mci/clc_rcv
```

`receive-uri`는 Tool이 호출할 주소가 아니라, MCI가 비동기/콜백 방식으로 응답을 전달하도록 별도 계약된 경우 수신에 사용하는 경로입니다.
## 10. 품질 검증

```powershell
.\gradlew.bat :dat-was-lib:test
.\gradlew.bat :dat-was-cus:compileJava
.\gradlew.bat :dat-was-sal:compileJava
.\gradlew.bat :dat-was-pro:compileJava
.\gradlew.bat :dat-was-sys:compileJava
.\gradlew.bat validateMcpToolNames
```

배포 전에는 다음을 확인합니다.

- Tool 이름의 전역 중복 여부와 `pod_domain_service_action` 규칙(아래 확인 사항 반영 후)
- Request/Response Schema 및 실제 예제 JSON
- Tool Pod 단위 테스트와 Gateway 경유 호출
- MCI/EAI 오류 코드의 사용자용 응답 매핑
- 민감정보가 요청·응답·로그에 포함되지 않는지
- 승인된 개발/테스트 계정과 데이터만 사용했는지

## 11. 보안과 운영 원칙

- API Key, Runner 등록 토큰, 계정·비밀번호 등 비밀값은 Git에 넣지 않고 환경 변수 또는 Secret Manager로 주입합니다.
- `TESTER-DEV` 같은 테스트 권한은 개발 환경에서만 허용하고, 운영에서는 Tool 단위 최소 권한으로 제한합니다.
- Tool 요청·응답 로그는 마스킹/제외 정책을 거친 값만 남깁니다.
- Trace ID는 요청 흐름 전체를 추적하고, Request ID는 HTTP 호출 단위로 새로 부여합니다.
- Tool Pod는 업무 소유권과 장애 격리 단위에 맞춰 독립 이미지·배포·운영 책임으로 분리합니다.

## 12. 참고 소스 위치

| 기능 | 위치 |
|---|---|
| Tool 공통 Controller | `dat-was-lib/src/main/java/io/shinhanlife/dat/mcc/presentation` |
| Tool 어노테이션 | `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/annotation` |
| JSON Schema/검증 | `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/schema`, `.../validation` |
| Manifest | `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/manifest` |
| Tool Scaffold | `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java` |
| Gateway 라우터 | `dat-gateway/src/main/java/io/shinhanlife/dat/mcg/presentation/McpRouterController.java` |
| Customer Tool | `dat-was-cus/src/main/java` |
| Sales Tool | `dat-was-sal/src/main/java` |
| Process Tool | `dat-was-pro/src/main/java` |
| System Tool | `dat-was-sys/src/main/java` |
## 13. 모듈명 전환 검증

Tool 관련 공통 기능은 `dat-was-*` 모듈명만 기준으로 동작합니다.

- `validateMcpToolNames`는 `dat-was-*` Tool Pod를 탐색하여 이름 규칙과 전역 중복을 검사합니다.
- Tool Scaffold는 Pod 이름을 Tool 함수명에 포함하지 않습니다. 함수명은 `도메인_비즈니스_행위` 형식입니다. 예: `cmm_notification_send`
- Pod Scaffold와 Gateway Scaffold 화면/API의 모듈 목록도 `dat-was-*` 명칭으로 통일되어 있습니다.
- Tool Source Update 기능은 `dat-was-*` 아래의 `*UseCase.java`를 검색합니다.

## 14. BC-DAB-STD-003 Tool Schema V17 적용

각 Tool의 표준 명세는 Tool Pod별 다음 경로에서 관리합니다.

```text
dat-was-{pod}/src/main/resources/tool-definitions/{categoryKey}/{toolName}.yml
```

Tool 이름은 Pod 정보를 포함하지 않는 `도메인_서비스_행위` 형태의 영문 소문자 snake_case를 사용하며,
정규식 `^[a-z][a-z0-9_]{2,63}$`을 만족해야 합니다. 예: `cmm_claim_search`.

필수 항목은 `name`, `display_name`, `version`, `category_key`, 설명 4개 요소(function, when_to_use,
when_not_to_use, io_limits), `display_description`, 예시 질의 3~10건, 동작 힌트 3개(read_only,
destructive, idempotent), `parameters_schema`입니다. 입력 Schema는 루트 `type: object`, 각 property의
`description`, `additionalProperties: false`를 갖춰야 합니다. 선택 운영 항목은 `tags`,
`legacy_interface_id`, `required_env_keys`, `owner_org`입니다.

기동 시 `tool-definitions/**/*.yml`을 한 번 읽어 이름 기준으로 캐시하고, `@McpTool` 실행 정보와 결합한
동일한 `ToolMetadata`를 `/tool-manifest`, Tool Pod MCP, Gateway MCP에 사용합니다.

입력 Schema 우선순위는 `inputSchemaResource` → V17 `parameters_schema` → DTO 자동 생성이고, 출력은
`outputSchemaResource` → 명시 Output Schema → `@McpOutputSchema` 기반 생성입니다. Output Schema를
명시한 Tool만 최종 응답 검증을 수행합니다. 현재 Scaffold는 Tool Definition V17 YAML과 응답 Mock JSON을
자동 생성하지 않고, 생성한 Java Tool의 `@GrowToolHint`를 기준으로 메타데이터를 제공합니다.

Scaffold의 Pod Module 생성은 `local`, `dev`, `test`, `prod` 프로필 파일과
`src/main/resources/tool-service-manifest.yml`을 만듭니다. 이 파일은 라우팅용 설정이며 `/tool-manifest`
API 응답을 대체하지 않습니다. AI 초안은 한국어 YAML로 생성하며, 선택한 Target Module 목록에서 자기
자신을 제외해 `confusable-servers`에 반영합니다. Tool Function의 기본 Protocol은 MCI(Legacy)입니다.

```powershell
.\gradlew.bat validateMcpToolNames validateToolSchemaV17
```

`bootJar`는 두 검증에 의존하므로 이름 중복, 필수 항목 누락, Java Tool과 YAML 명세 불일치가 있으면
Docker 이미지 생성 전에 빌드가 실패합니다.
