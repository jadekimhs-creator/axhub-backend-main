# AXHUB Backend

Spring Boot 기반 AXHUB 관리자 백엔드 API 서버 및 MCP(Model Context Protocol) Gateway / Tool 분산 서버 프로젝트입니다.

---

##  아키텍처 개요 (Architecture Overview)

AXHUB Backend는 3개의 주요 애플리케이션으로 분리 운영됩니다:

1. **AXHUB Admin (`AxHubAdminApplication`)**: 관리자 웹 화면을 위한 REST API 서버
2. **MCP Gateway (`AxHubGatewayApplication`)**: 외부 LLM(Claude, GPT 등) 서버의 MCP 통신을 받아, 내부 Tool 서버들로 분배(라우팅)하는 허브 서버 (포트: 8081)
3. **MCP Tool (`AxHubToolApplication`)**: 실제 레거시 시스템(MCI, EAI 등)과 통신하여 비즈니스 로직(결제, 휴가신청 등)을 수행하는 어댑터 서버 (포트: 8082~8084 분산 구성 가능)

---

##  환경 (Environment)

| 항목 | 버전 |
|------|------|
| Java | 21 |
| Spring Boot | 4.0.5 |
| Build Tool | Gradle |
| 주요 기술 스택 | MyBatis, Lombok, MapStruct, P6Spy |
| 데이터베이스 | H2 (in-memory, 로컬 개발용) |
| 세션/캐시 저장소 | Redis |
| **장애 격리 / 제어** | **Resilience4j (RateLimiter, CircuitBreaker, Retry)** |
| **메시지 큐** | **Kafka (트래픽 폭주 시 대기열 전환용)** |

---

## ▶ 실행 방법 (How to Run)

### 1. Gateway & Tool 서버 실행 (MCP 연동용)
- **Gateway 서버 기동:**
  - `./gradlew bootRun -PmainClass=io.shinhanlife.AxHubGatewayApplication` (기본 포트: 8081)
- **Tool 서버 기동 (필요에 따라 N대 스케일 아웃 가능):**
  - `./gradlew bootRun -PmainClass=io.shinhanlife.AxHubToolApplication --args="--server.port=8082"`
  - Tool 서버가 기동되면 자동으로 Gateway(8081)에 자신을 등록(Auto-Registration)합니다.
  - **(선택) 특정 Tool 그룹만 실행하기:** 
    - 업무 특성에 따라 세분화된 그룹에 속한 Tool만 띄우고 싶다면, 실행 인수에 `--mcp.tool.target=그룹명`을 추가합니다.
    - **지원되는 그룹명:**
      - `NOTIFICATION`: 이메일, SMS 발송
      - `CLAIM`: 청구 처리, 심사 상태 조회
      - `POLICY`: 증권 발행, 발행 가능 여부 조회
      - `HR`: 휴가 등록, 연차 갯수 조회
      - `CONTRACT`: 계약 상태, 계약 상세 조회
      - `CUSTOMER`: 고객 등급, 고객 상세 정보 조회
    - IntelliJ IDEA: `Run/Debug Configurations`  `AxHubToolApplication`  `Program arguments` 에 `--mcp.tool.target=NOTIFICATION` 입력

### 2. Admin 관리자 서버 실행
- **Admin 서버 기동:**
  - `./gradlew bootRun -PmainClass=io.shinhanlife.AxHubAdminApplication` (포트: 8080)

---

##  Gemini MCP 연동 (Integration)

본 시스템은 REST API(JSON-RPC)를 사용하므로, **표준 MCP 통신(Stdio)**을 지원하기 위한 브릿지 스크립트를 내장하고 있습니다. 이를 통해 제미나이(Gemini Code Assist, Gemini CLI 등)와 코드 수정 없이 완벽히 연동됩니다.

- **표준 브릿지 파일**: `McpBridge.java` (루트 디렉토리)
- **제미나이 설정 방법**:
  제미나이 도구의 `settings.json` (또는 `mcp.json`)에 다음과 같이 프로세스를 등록합니다.
  ```json
  "mcpServers": {
    "axhub-gateway": {
      "command": "java",
      "args": ["/프로젝트절대경로/axhub-backend-main/McpBridge.java"]
    }
  }
  ```
  *(참고: Antigravity IDE 환경에서는 이미 `.agents/mcp.json`에 설정되어 자동 연동됩니다.)*

---

##  비공개 Tool 관리 및 Fallback 연동 (Visibility & Routing)

저희 시스템은 MSA 보안 및 아키텍처 원칙에 따라 Tool의 **레지스트리 등록 여부(라우팅)**와 **API 노출 여부(가시성)**를 완벽히 분리하여 관리합니다.

1. **`visible = false`**: 
   레지스트리에 정상적으로 등록되어 게이트웨이가 동적으로 라우팅하지만, 클라이언트에게 제공되는 `/tools/list` API 목록에서는 숨겨집니다.
2. **`register = false`**: 
   내부 레지스트리(Redis)에 툴 정보를 등록하지 않습니다 (외부 레지스트리를 독자적으로 사용할 경우 등). 
   이 경우 게이트웨이는 `application.properties`의 `mcp.gateway.fallback.routes` 설정을 참조하여 **Fallback 정적 라우팅**을 수행하므로 연동이 100% 보장됩니다.

```java
@McpFunction(
    name = "secret_tool",
    visible = false, // 목록 숨김 여부 (기본값: true)
    register = false // 내부 Redis 등록 여부 (기본값: true)
)
```

---

##  안정성 및 트래픽 제어 (Resilience4j)

MSA(Microservices Architecture) 환경의 안정성을 위해 완벽한 2-Track 방어막을 구축했습니다.
1. **Gateway 계층 (동적 방어):** Tool이 등록할 때 제출한 메타데이터(SLA)를 기반으로 Gateway 내에서 동적 CircuitBreaker 및 RateLimiter를 가동합니다. 한계치 초과 시 트래픽을 Kafka 큐로 비동기 전환합니다.
2. **Tool 계층 (정적 방어):** 레거시 시스템(EIMS/MCI)과 통신하는 커넥터 내부에 `@CircuitBreaker`, `@RateLimiter` 어노테이션이 적용되어 장애 전파를 차단합니다.

---

##  모듈(Pod) 및 Tool 코드 자동 생성 (Scaffolders)

새로운 도메인의 기능을 추가할 때 발생하는 반복적인 설정(보일러플레이트, 설정 파일 복사 등)을 1초 만에 자동화하기 위해 **AXHUB Developer Portal (Web UI)** 및 **CLI 스캐폴더 2종**을 제공합니다.

###  1. AXHUB Developer Portal (Web UI) - 가장 추천하는 방식!
이제 더 이상 터미널에서 명령어를 칠 필요가 없습니다. Gateway 모듈에 내장된 웹 화면에서 빈칸만 채우면 코드가 마법처럼 찍혀 나옵니다.

1. **접속 방법**: Gateway 서버 기동 후 브라우저에서 `http://localhost:8081/admin/scaffold.html` 접속
2. **Pod (모듈) 생성 탭**: 모듈명(예: hr)과 포트만 입력하면 독립적인 Spring Boot 모듈이 디렉토리부터 빌드 스크립트까지 완벽히 생성됩니다.
3. **Tool (기능) 생성 탭**: 생성된 모듈에 새로운 툴(서비스/DTO) 코드를 자동으로 주입합니다.

### 2. CLI 스캐폴더 (기존 터미널 방식)
웹 화면을 사용할 수 없는 환경이거나 터미널이 익숙한 경우, 아래 명령어를 통해 CLI 마법사를 사용할 수 있습니다.

### 1⃣ 새로운 Pod(모듈) 전체를 생성할 때: `PodScaffolder`
새로운 도메인(예: 결제, HR)을 위한 완전히 독립적인 Spring Boot 모듈을 생성합니다. 폴더 구조, 빌드 스크립트, 각종 프로퍼티 및 도커 설정까지 완벽하게 세팅됩니다.

```bash
# 사용법: javac로 컴파일 후 실행
javac -encoding UTF-8 axhub-common/src/main/java/io/shinhanlife/axhub/common/util/PodScaffolder.java
java -cp axhub-common/src/main/java io.shinhanlife.axhub.common.util.PodScaffolder [모듈명] [포트번호]

# 실행 예시 (axhub-tool-hr 모듈을 8086 포트로 생성)
java -cp axhub-common/src/main/java io.shinhanlife.axhub.common.util.PodScaffolder hr 8086
```

### 2⃣ 생성된 모듈에 새로운 툴(Function)을 추가할 때: `ToolScaffolder`
어노테이션(`@McpTool`, `@McpFunction`)이 완벽히 달린 Service와 입출력 DTO 코드를 지정된 모듈 패키지 룰에 맞춰 자동 생성합니다.

```bash
# 사용법: javac로 컴파일 후 실행
javac -encoding UTF-8 axhub-common/src/main/java/io/shinhanlife/axhub/common/util/ToolScaffolder.java
java -cp axhub-common/src/main/java io.shinhanlife.axhub.common.util.ToolScaffolder [Tool이름] [인터페이스ID] "[기능설명]" "[그룹명]" "[통신방식]" "[모듈명]"

# 실행 예시 (payment 모듈에 결제 승인 기능 추가)
java -cp axhub-common/src/main/java io.shinhanlife.axhub.common.util.ToolScaffolder PaymentApproval PAY_001 "결제 승인 처리 기능" "COMMON" "HTTP" "axhub-tool-payment"
```

---

##  패키지 구조 (Package Structure)

```text
axhub-backend-main (Root)
├── axhub-gateway           #  MCP 라우팅 허브 서버 (외부 LLM과 통신 및 Tool 분배)
├── axhub-common            # 공통 모듈 (Security, Session, Config 등)
├── axhub-tool-core         # Tool 공통 기능 (AbstractMcpToolService, Annotation, Scaffolder)
├── axhub-tool-email        # [Tool] 이메일 발송 특화 어댑터 모듈
├── axhub-tool-sms          # [Tool] SMS 발송 특화 어댑터 모듈
├── axhub-tool-payment      # [Tool] 결제 비즈니스 어댑터 모듈 (Scaffolded)
└── axhub-tool-other        # [Tool] 기타 비즈니스(청구, 계약, 고객, HR 등) 어댑터 모듈
```

*(참고: 기존 단일 모듈 프로젝트에서 마이크로서비스 확장을 위해 모듈별로 분리되었으며, 각 Tool 서버는 독립적으로 확장 및 배포할 수 있습니다.)*
