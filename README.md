# AXHUB Backend

Spring Boot 기반 AXHUB 관리자 백엔드 API 서버 및 MCP(Model Context Protocol) Gateway / Tool 분산 서버 프로젝트입니다.

---

## 🚀 아키텍처 개요 (Architecture Overview)

AXHUB Backend는 3개의 주요 애플리케이션으로 분리 운영됩니다:

1. **AXHUB Admin (`AxHubAdminApplication`)**: 관리자 웹 화면을 위한 REST API 서버
2. **MCP Gateway (`AxHubGatewayApplication`)**: 외부 LLM(Claude, GPT 등) 서버의 MCP 통신을 받아, 내부 Tool 서버들로 분배(라우팅)하는 허브 서버 (포트: 8081)
3. **MCP Tool (`AxHubToolApplication`)**: 실제 레거시 시스템(MCI, EAI 등)과 통신하여 비즈니스 로직(결제, 휴가신청 등)을 수행하는 어댑터 서버 (포트: 8082~8084 분산 구성 가능)

---

## 🛠 환경 (Environment)

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

## ▶️ 실행 방법 (How to Run)

### 1. Gateway & Tool 서버 실행 (MCP 연동용)
- **Gateway 서버 기동:**
  - `./gradlew bootRun -PmainClass=io.shinhanlife.AxHubGatewayApplication` (기본 포트: 8081)
- **Tool 서버 기동 (필요에 따라 N대 스케일 아웃 가능):**
  - `./gradlew bootRun -PmainClass=io.shinhanlife.AxHubToolApplication --args="--server.port=8082"`
  - Tool 서버가 기동되면 자동으로 Gateway(8081)에 자신을 등록(Auto-Registration)합니다.

### 2. Admin 관리자 서버 실행
- **Admin 서버 기동:**
  - `./gradlew bootRun -PmainClass=io.shinhanlife.AxHubAdminApplication` (포트: 8080)

---

## 🛡️ 안정성 및 트래픽 제어 (Resilience4j)

MSA(Microservices Architecture) 환경의 안정성을 위해 완벽한 2-Track 방어막을 구축했습니다.
1. **Gateway 계층 (동적 방어):** Tool이 등록할 때 제출한 메타데이터(SLA)를 기반으로 Gateway 내에서 동적 CircuitBreaker 및 RateLimiter를 가동합니다. 한계치 초과 시 트래픽을 Kafka 큐로 비동기 전환합니다.
2. **Tool 계층 (정적 방어):** 레거시 시스템(EIMS/MCI)과 통신하는 커넥터 내부에 `@CircuitBreaker`, `@RateLimiter` 어노테이션이 적용되어 장애 전파를 차단합니다.

---

## 🧰 MCP Tool 코드 자동 생성 (ToolScaffolder)

반복적인 Tool 모듈 생성 작업을 자동화하기 위해 **CLI 스캐폴더**를 제공합니다.
다음 명령어를 터미널에 입력하면, Service 및 DTO 보일러플레이트 코드가 패키지 룰에 맞춰 자동 생성됩니다.

```bash
# 사용법: javac로 컴파일 후 실행
javac -encoding UTF-8 src/main/java/io/shinhanlife/axhub/biz/mcp/tool/util/ToolScaffolder.java
java -cp src/main/java io.shinhanlife.axhub.biz.mcp.tool.util.ToolScaffolder [Tool이름] [인터페이스ID] "[기능설명]" "[그룹명]" "[통신방식]"

# 실행 예시
java -cp src/main/java io.shinhanlife.axhub.biz.mcp.tool.util.ToolScaffolder ExchangeRate EXCH_001 "환율 조회 기능" "group_1" "HTTP"
```

---

## 📂 패키지 구조 (Package Structure)

```
io.shinhanlife
├── AxHubAdminApplication.java
├── AxHubGatewayApplication.java  ← MCP 라우팅 허브
├── AxHubToolApplication.java     ← 비즈니스 어댑터 (레거시 통신)
│
├── axhub/
│   ├── biz/                      
│   │   ├── sm/                   # 관리자 메뉴 관리 도메인
│   │   ├── so/                   # 관리자 접근 권한 도메인
│   │   └── mcp/                  # 💡 [MCP 도메인] Gateway 및 Tool 로직 분리
│   │       ├── gateway/          # API Key 인증, Tool 자동 등록, RPC 라우팅 처리
│   │       ├── tool/             # 레거시 EIMS/MCI 통신 Service 및 DTO
│   │       └── adapter/          # TCP/HTTP/ESB 레거시 모의(Mock) 서버 
│   │
│   ├── common/                   # 공통 모듈 (Security, Session, Config 등)
│   └── sample/                   # 개발 참고용 샘플
│
└── glow/                         # 사내 표준 Glow 프레임워크 호환 패키지
```

각 관리자 업무 패키지는 기존처럼 `presentation`, `usecase`, `dto`, `domain`, `converter` 5계층 아키텍처를 엄격하게 따릅니다.
