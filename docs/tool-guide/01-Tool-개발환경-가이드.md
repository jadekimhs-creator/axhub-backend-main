# AX HUB Tool 개발 환경 가이드

## 1. 문서 목적

이 문서는 AX HUB의 **Tool Pod 개발자**가 로컬 PC와 신한라이프 내부망에서 동일한 방식으로 소스를 빌드하고 실행하기 위한 기준을 정리한다.

이 문서의 범위는 `dat-was-lib`와 Tool Pod(`dat-was-cus`, `dat-was-sal`, `dat-was-pro`, `dat-was-sys`)이다. 외부 라우팅 계층의 구성과 운영 방법은 다루지 않는다.

## 2. 기준 기술 환경

| 항목 | 현재 기준 | 용도 |
|---|---:|---|
| Java | 21 | 컴파일 및 Tool Pod 실행 |
| Spring Boot | 3.5.11 | Tool Pod 애플리케이션 기반 |
| Gradle Wrapper | 8.14.3 | 빌드 표준화 |
| MCP Java SDK | 2.0.0 | MCP 서버와 JSON 전송 처리 |
| Spring AI BOM | 1.1.8 | Boot 3.5 계열 의존성 정렬 |
| MCP Annotations | 0.9.0 | `@McpTool`, `@McpToolParam` 제공 |
| MapStruct | 1.5.5.Final | Tool DTO와 연계 전문 변환 |
| Lombok | 1.18.32 | DTO·생성자 반복 코드 축소 |
| JSON Schema Validator | 2.0.0 | MCP SDK 2.0.0 호환 Schema 검증 |

> `com.networknt:json-schema-validator`는 반드시 현재 지정된 `2.0.0`을 유지한다. 3.x를 혼용하면 MCP SDK가 기대하는 `Schema.validate(JsonNode)` 규격과 달라 `NoSuchMethodError`가 발생할 수 있다.

## 3. Tool 모듈 구성

| 모듈 | 기본 포트 | 역할 |
|---|---:|---|
| `dat-was-lib` | 해당 없음 | MCP 서버 설정, Tool 탐색·실행, Schema, V17 메타데이터, MCI·HTTP 연동, 공통 로그·헤더 처리 |
| `dat-was-cus` | 8084 | 고객 관련 Tool Pod |
| `dat-was-sal` | 8082 | 영업 관련 Tool Pod |
| `dat-was-pro` | 8085 | 상품 관련 Tool Pod |
| `dat-was-sys` | 8086 | 시스템 관련 Tool Pod |

각 실행 모듈은 `dat-was-lib`를 의존하고 다음 패키지를 스캔한다.

```java
@SpringBootApplication(scanBasePackages = {
    "io.shinhanlife.dat.mcc",
    "io.shinhanlife.dat.lib"
})
@Import(ToolMcpServerConfiguration.class)
```

따라서 업무 Tool은 `io.shinhanlife.dat.mcc` 아래에, 공통 기능은 `io.shinhanlife.dat.lib` 아래에 둔다.

## 4. 개발 PC 준비

### 4.1 필수 설치 항목

1. JDK 21
2. IntelliJ IDEA 또는 Eclipse 기반 개발도구
3. Git
4. Docker Desktop 또는 사내 표준 컨테이너 실행 환경(통합 테스트 시)

별도 Gradle 설치는 필요하지 않다. 저장소에 포함된 `gradlew.bat`를 사용한다.

### 4.2 IntelliJ 설정

- Project SDK: JDK 21
- Gradle JVM: JDK 21
- Build and run using: Gradle 권장
- File Encoding: UTF-8
- Annotation Processing: 활성화
- 줄바꿈: Git 정책에 맞춰 유지

한글 주석이나 문자열이 깨지는 경우 파일을 UTF-8로 다시 저장하고, Java 파일 첫 바이트에 BOM(`\ufeff`)이 들어가지 않았는지 확인한다. Java 소스는 **UTF-8 without BOM**을 사용한다.

## 5. 프로파일과 설정 파일

각 Tool Pod는 다음 구조를 사용한다.

```text
src/main/resources/
├─ application.yml
├─ application-local.yml
├─ application-dev.yml
├─ application-test.yml
└─ application-prod.yml
```

프로파일 파일은 공통 라이브러리의 Glow 설정을 가져온다.

```yaml
spring:
  config:
    activate:
      on-profile: local
    import:
      - classpath:glow/application-glow.yml
      - classpath:glow/application-glow-local.yml
```

| 프로파일 | 용도 |
|---|---|
| `local` | 개발자 PC, H2 및 로컬 Mock 사용 |
| `dev` | 개발계 MCI·HTTP 연계 |
| `test` | 테스트 환경 연계 |
| `prod` | 운영 환경 연계 |

실행 프로파일은 다음 중 하나로 지정한다.

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\gradlew.bat :dat-was-cus:bootRun
```

또는 IntelliJ Run Configuration의 `Active profiles`에 `local`을 입력한다.

## 6. 주요 환경 변수

환경별 주소와 보안 값은 Java 코드에 직접 작성하지 않고 환경 변수로 주입한다.

| 환경 변수 | 의미 | local 기본값 예시 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 실행 프로파일 | `local` |
| `PORT` | Tool Pod 포트 | 모듈별 기본 포트 |
| `AXHUB_TOOL_URL` | 현재 Tool Pod의 공개 기준 URL | `http://localhost:${server.port}` |
| `GLOW_COMMUNICATION_MCI_HOST` | MCI 호스트 | `http://localhost` |
| `GLOW_COMMUNICATION_MCI_PORT` | MCI 포트 | `8080` |
| `AXHUB_{API_NAME}_HTTP_DOMAIN` | HTTP API 도메인 | Tool별 설정값 |
| `AXHUB_{API_NAME}_HTTP_URL` | HTTP API 경로 | Tool별 설정값 |

비밀번호, 인증키, 암호화키는 YAML 기본값으로 커밋하지 않는다.

## 7. 빌드와 실행

### 7.1 공통 라이브러리 검증

```powershell
.\gradlew.bat :dat-was-lib:clean :dat-was-lib:test
```

### 7.2 Tool Pod 컴파일

```powershell
.\gradlew.bat :dat-was-cus:compileJava
.\gradlew.bat :dat-was-sal:compileJava
.\gradlew.bat :dat-was-pro:compileJava
.\gradlew.bat :dat-was-sys:compileJava
```

### 7.3 Tool Pod 실행

```powershell
.\gradlew.bat :dat-was-cus:bootRun
```

실행 후 확인할 Tool Pod 자체 주소는 다음과 같다.

| 기능 | 주소 예시 |
|---|---|
| MCP 표준 엔드포인트 | `http://localhost:8084/mcp` |
| Tool Manifest | `http://localhost:8084/tool-manifest` |
| Tool 테스트 콘솔 | `http://localhost:8084/tool-test-console.html` |
| Tool 직접 실행 | `POST http://localhost:8084/mcp/{toolName}` |

포트는 실행한 Pod에 맞게 바꾼다.

## 8. Redis와 Docker 사용 기준

- Tool DTO 작성, 컴파일, 단위 테스트에는 Docker가 필요하지 않다.
- 로컬 HTTP Mock이나 MCI Mock을 사용할 때만 관련 컨테이너를 실행한다.
- Redis가 없어도 Tool 목록과 Tool 자체 실행을 확인할 수 있도록 개발한다.
- Redis를 사용하는 부가 기능을 시험할 때만 Redis를 실행한다.

즉, Tool 개발의 최소 실행 단위는 **JDK 21 + 해당 Tool Pod**이다.

## 9. 내부망 반입 준비

신한라이프 내부망에는 다음 항목을 사전에 준비한다.

1. JDK 21 또는 Java 21 컨테이너 이미지
2. Gradle Wrapper 8.14.3 배포 파일
3. 사내 Nexus의 전체 Maven 의존성
4. Glow Framework 사내 JAR와 그 전이 의존성
5. MCP SDK 2.0.0 및 JSON Schema Validator 2.0.0
6. 사내 인증서와 JVM TrustStore
7. MCI·HTTP 대상 주소, 포트, ACL
8. 환경별 설정값과 Secret 주입 방식

컨테이너 빌드 시 사내 Registry에 Java 21 빌드·실행 이미지가 미러링되어 있어야 한다. 이미지 경로는 Dockerfile의 `FROM`만 사내 경로로 치환하고, Java 버전은 21로 유지한다.

## 10. 환경 점검 체크리스트

- [ ] `java -version`이 21이다.
- [ ] Gradle JVM이 JDK 21이다.
- [ ] `gradlew.bat --version`이 정상 실행된다.
- [ ] `:dat-was-lib:test`가 성공한다.
- [ ] 대상 Tool Pod의 `compileJava`가 성공한다.
- [ ] local 프로파일로 Tool Pod가 기동된다.
- [ ] `/tool-manifest`에서 Tool 목록이 조회된다.
- [ ] `/tool-test-console.html`에서 샘플 Tool이 실행된다.
- [ ] MCI·HTTP 연계 주소와 ACL이 환경별로 준비되어 있다.
- [ ] 소스와 설정 파일의 한글이 UTF-8로 정상 표시된다.

