# AXHUB Backend

Spring Boot 기반 AXHUB 관리자 백엔드 API 서버 및 MCP(Model Context Protocol) Gateway / Tool 분산 서버 프로젝트입니다.

---

##  아키텍처 개요 (Architecture Overview)

AXHUB Backend는 크게 2개의 주요 애플리케이션 계층으로 분리 운영됩니다:

1. **MCP Gateway (`AxHubGatewayApplication`)**: 내장된 웹 UI(AI 챗봇, 스캐폴더) 제공 및 외부 LLM(Claude, GPT 등) 서버의 MCP 통신을 받아 내부 Tool 서버들로 분배(라우팅)하는 허브 서버 (포트: 8081)
2. **MCP Tool (`AxHubToolApplication`)**: 실제 레거시 시스템(MCI, EAI 등)과 통신하여 비즈니스 로직(결제, 휴가신청 등)을 수행하는 어댑터 서버 (포트: 8082~8085 등 분산 구성 가능)

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

### 1. Docker Compose를 이용한 전체 실행 (권장)
마이크로서비스 아키텍처 특성상 여러 개의 Tool Pod이 필요하므로, Docker Compose를 이용한 전체 실행을 권장합니다.

```bash
# 전체 시스템(Gateway + Redis + 모든 Tool Pod) 빌드 및 백그라운드 실행
./gradlew build -x test
docker compose up -d --build
```
- **Gateway (Chat UI & 라우터)**: `http://localhost:8281`
- Gateway가 뜨면 내장된 챗봇 웹 UI(`http://localhost:8281/chat.html`)에 접속하여 바로 테스트할 수 있습니다.

### 2. 로컬 개발 시 개별 실행 (IntelliJ / Gradle)
개발 중 특정 모듈만 띄워 디버깅해야 할 경우 아래와 같이 실행합니다.

- **Gateway 서버 기동:**
  - `./gradlew :axhub-gateway:bootRun`
- **Tool 서버 기동 (예: other 툴):**
  - `./gradlew :axhub-tool-other:bootRun`
  - Tool 서버가 기동되면 자동으로 Gateway에 자신을 등록(Auto-Registration)합니다.



---

## 🤖 AI Agent 연동 아키텍처 (Spring AI & MCP)

본 시스템은 **투트랙(Two-Track) AI 연동 아키텍처**를 제공하여 로컬 개발 환경과 프로덕션 환경 모두를 완벽하게 지원합니다.

### 1. 내장형 웹 챗봇 (Spring AI 기반) - NEW! 🎉
가장 빠르고 직관적으로 AI 에이전트를 테스트할 수 있는 내장형 챗봇 화면을 제공합니다. Gateway 서버 자체에 **Spring AI (spring-ai-starter-mcp-server-webmvc)** 가 연동되어 있어 별도의 파이썬 스크립트나 외부 앱 없이도 즉각적인 테스트가 가능합니다.

- **접속 방법**: Gateway(Docker) 기동 후 브라우저에서 `http://localhost:8281/chat.html` 접속
- **동작 방식**: 
  1. 사용자가 질문을 입력하면 내부 `ChatClient` (Gemini API 등)로 전송
  2. Spring AI가 내부 레지스트리의 Tool 목록을 분석하여 필요한 Tool 탐색
  3. LLM이 Tool 호출 판단 시, Gateway의 `ExecuteService`를 거쳐 Tool Pod의 기능을 직접 실행
  4. 결과를 LLM이 다시 해석하여 사용자에게 자연어로 응답

### 2. 프로덕션 클라우드 AI (Google Cloud Agent Builder 등) 연동
실제 라이브 서비스에서 동작하는 클라우드 Agent Builder는 REST API 기반의 OpenAPI Spec을 요구합니다. 
`axhub-gateway`는 이미 **Agent Builder 규격의 REST API(`/mcp/api/v1/tools/call`)를 네이티브로 제공**하므로, 별도의 브릿지나 어댑터 없이 Endpoint URL과 Swagger(OpenAPI) 문서만 클라우드 콘솔에 등록하면 즉시 라이브 챗봇/에이전트로 서비스할 수 있습니다.

### 3. (Legacy) 로컬 코딩 AI (Cursor, Claude Desktop 등) 연동
표준 MCP 통신(Stdio)을 요구하는 로컬 AI 에이전트를 위해 자바 기반의 브릿지 스크립트(`McpBridge.java`)를 내장하고 있습니다. 브릿지가 Stdio 요청을 HTTP로 변환하여 로컬 환경의 Gateway로 전달합니다.

- **설정 방법**: IDE의 `mcp_config.json` 설정 파일에 아래와 같이 등록합니다.
  ```json
  "mcpServers": {
    "axhub-gateway": {
      "command": "java",
      "args": ["C:/절대경로/axhub-backend-main/McpBridge.java"]
    }
  }
  ```

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

## 🛡️ 시스템 안정성 및 네트워크 제어 (Resilience & Network)

MSA 및 외부 시스템(MCI) 연동 환경의 안정성을 위해 완벽한 3-Tier 방어 체계를 구축했습니다.

1. **Gateway 라우팅 방어 (Timeout & Fallback):**
   - MCP 라우터(`McpRouterController`) 단에 1초 타임아웃을 강제 적용하여 특정 Tool Pod의 응답 지연이 전체 시스템 장애로 이어지는 것을 방지하고 신속하게 정적 Fallback 라우팅으로 전환합니다.
2. **MCI 네트워크 안정화 (HTTP/1.1 Downgrade):**
   - 기존 HTTP/2 사용 시 레거시 시스템 연동 중 간헐적으로 발생하던 `RST_STREAM` 오류를 원천 차단하기 위해, MCI 전용 `HttpEimsSender`에는 고도로 최적화된 **HTTP/1.1 전용 커넥션 풀(Factory)**이 고정 적용되어 네트워크 단절을 방지합니다.
3. **Resilience4j 기반 트래픽 제어:**
   - **Gateway 계층 (동적 방어):** Tool 등록 시 제출된 SLA 메타데이터를 기반으로 동적 CircuitBreaker 및 RateLimiter를 가동하며, 한계치 초과 시 Kafka 큐로 비동기 전환합니다.
   - **Tool 계층 (정적 방어):** 레거시 커넥터 내부에 `@CircuitBreaker`, `@RateLimiter` 어노테이션 기반의 장애 전파 차단 로직이 2차적으로 가동됩니다.

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

# --- ���Ѷ����� EAI/MCI ���� IP ���� (���� ȯ��) ---
shinhan.integration.envrTypeCd=D
shinhan.integration.eai.url=http://10.176.32.181
shinhan.integration.internalMci.url=http://10.176.32.173
shinhan.integration.bancaMci.url=http://10.176.32.117
shinhan.integration.externalMci.url=http://10.176.32.176

# --- ���Ѷ����� EAI/MCI ���� IP ���� (�׽�Ʈ ȯ��) ---
shinhan.integration.envrTypeCd=T
shinhan.integration.eai.url=http://10.174.32.181
shinhan.integration.internalMci.url=http://10.174.32.173
shinhan.integration.bancaMci.url=http://10.174.32.117
shinhan.integration.externalMci.url=http://10.176.32.177

# --- ���Ѷ����� EAI/MCI ���� IP ���� (� ȯ��) ---
shinhan.integration.envrTypeCd=R
shinhan.integration.eai.url=http://10.172.32.181
shinhan.integration.internalMci.url=http://10.172.32.173
shinhan.integration.bancaMci.url=http://10.172.32.117
shinhan.integration.externalMci.url=http://10.172.32.177
