# AX HUB Tool 개발 가이드 1차본

## 1. 목적과 적용 범위

이 문서는 신규 Tool을 설계한 뒤 실제 소스에 구현하고 단위·연계 테스트까지 완료하는 절차를 설명한다.

대상은 다음 Tool 모듈이다.

- `dat-was-cus`
- `dat-was-sal`
- `dat-was-pro`
- `dat-was-sys`
- 공통 기능이 필요한 경우 `dat-was-lib`

## 2. 개발 전 준비 자료

Tool 개발 착수 전에 다음 내용을 확보한다.

1. Tool 업무명과 자연어 사용 예시
2. 입력·출력 항목 정의
3. 필수값, 형식, 길이, 코드값
4. 조회/변경 여부와 사용자 승인 필요 여부
5. MCI 인터페이스 ID 또는 HTTP API명
6. 요청·응답 전문 및 오류 코드 정의
7. 개발·테스트 환경 endpoint와 ACL
8. 개인정보 포함 여부와 처리 기준

## 3. 대상 Tool Pod 선택

| Pod | 선택 기준 | 기본 포트 |
|---|---|---:|
| CUS | 고객 업무 | 8084 |
| SAL | 영업 업무 | 8082 |
| PRO | 상품 업무 | 8085 |
| SYS | 시스템·공통 시스템 업무 | 8086 |

Tool명에는 Pod명을 넣지 않는다. Pod는 배포와 장애 격리 단위이고 Tool명은 업무 기능 식별자다.

## 4. 구현 방식 선택

| 방식 | 선택 기준 | 핵심 Client |
|---|---|---|
| MCI | 인터페이스 ID와 정형 전문으로 연계 | `Mci{SystemCode}Client` |
| HTTP | JSON 기반 사내 API로 연계 | `{HttpApiName}Client` |
| 내부 로직 | 외부 연계 없이 계산·조회 가능 | UseCaseImpl 내부 서비스 |

외부 연계가 있어도 Tool UseCase의 입출력은 항상 AI가 이해할 수 있는 업무 DTO로 유지한다.

## 5. Scaffold로 기본 소스 생성

Scaffold 입력 시 최소한 다음 값을 정확히 지정한다.

| 항목 | 예시 | 설명 |
|---|---|---|
| Target Module | `dat-was-sal` | 소스가 생성될 Tool Pod |
| Domain Category | `cmm` | 업무 패키지 및 Schema 경로 |
| Base Name | `ClaimSearch` | Java 클래스명 기준 |
| Tool Name | `cmm_claim_search` | MCP Tool 식별자 |
| Title | `보험금 청구 상태 조회` | 화면 표시용 짧은 명칭 |
| Description | `청구번호 또는 계약번호로 상태를 조회합니다.` | 기능 설명 |
| Routing Type | `MCI` 또는 `HTTP` | 연계 방식 |
| Interface ID | `CLCNNB00001` | MCI 선택 시 |
| Client System Code | `CFPA` | MCI Client 패키지/이름 기준 |
| HTTP API Name | `insurance` | HTTP 설정과 Client 연결 키 |

기존 UseCase에 함수를 추가할 경우 대상 UseCase를 선택한다. Scaffold는 UseCase 인터페이스와 UseCaseImpl 양쪽에 같은 메서드를 추가해야 한다. MCI뿐 아니라 HTTP와 내부 로직도 여러 함수 구성을 지원해야 한다.

## 6. 생성 결과 확인

### 6.1 공통 업무 파일

```text
biz/{category}/dto/{BaseName}Request.java
biz/{category}/dto/{BaseName}Response.java
biz/{category}/converter/{BaseName}Converter.java
biz/{category}/usecase/{BaseName}UseCase.java
biz/{category}/usecase/impl/{BaseName}UseCaseImpl.java
```

### 6.2 MCI 선택 시

```text
infra/itrf/mci/{clientSystemCode}/Mci{ClientSystemCode}Client.java
infra/itrf/mci/{clientSystemCode}/io/{InterfaceId}_I.java
infra/itrf/mci/{clientSystemCode}/io/{InterfaceId}_O.java
```

### 6.3 HTTP 선택 시

```text
infra/itrf/http/{httpApiName}/{HttpApiName}Client.java
infra/itrf/http/{httpApiName}/io/{BaseName}HttpRequest.java
infra/itrf/http/{httpApiName}/io/{BaseName}HttpResponse.java
```

### 6.4 리소스

```text
tool-definitions/{categoryKey}/{toolName}.yml
tool-schemas/{categoryKey}/*-input-schema.json
tool-schemas/{categoryKey}/*-output-schema.json
mock-responses/{toolName}.json
```

복잡한 Schema를 사용하지 않으면 JSON Schema 파일은 생략할 수 있다.

## 7. DTO 작성

### 7.1 Request DTO

- 자연어에서 추출할 수 있는 업무 용어로 필드명을 정한다.
- 필수값과 예시를 명시한다.
- 날짜, 금액, 코드의 형식을 description에 적는다.
- 중첩 구조는 의미 있는 inner class 또는 별도 DTO로 만든다.
- `List<String>`과 `List<Object>`를 구분하고 복합 배열은 요소 필드를 정의한다.

### 7.2 Response DTO

- 레거시 전문 전체가 아닌 사용자에게 필요한 정보만 반환한다.
- 상태 코드와 상태명을 같이 제공한다.
- null과 빈 문자열을 구분한다.
- 배열 정렬 기준과 추가 결과 존재 여부를 설명한다.
- Output Schema를 사용할 때 실제 반환값이 Schema의 타입과 required 조건을 만족해야 한다.

## 8. Converter 구현

Converter는 다음 두 방향을 담당한다.

```text
Tool Request  → MCI/HTTP Request
MCI/HTTP Response → Tool Response
```

MapStruct를 기본으로 사용하되, 날짜·코드·중첩 객체처럼 자동 매핑이 어려운 항목은 명시적으로 변환한다.

```java
@Mapper(componentModel = "spring")
public interface ClaimSearchConverter {
    CLCNNB00001_I toMciRequest(ClaimSearchRequest request);
    ClaimSearchResponse toResponse(CLCNNB00001_O response);
}
```

필드명이 같더라도 중요한 업무 값은 테스트로 매핑 결과를 확인한다.

## 9. MCI Tool 구현 절차

1. 인터페이스 ID와 요청·응답 전문을 확정한다.
2. `{InterfaceId}_I`, `{InterfaceId}_O`에 전문 구조를 구현한다.
3. Converter에서 Tool DTO와 전문 DTO를 변환한다.
4. `Mci{SystemCode}Client`가 `AxhubMciComponent`를 호출하도록 한다.
5. UseCaseImpl은 변환된 전문을 Client에 전달한다.
6. 응답 전문을 Tool Response로 변환한다.
7. 전문 오류 코드와 통신 예외를 사용자용 오류로 매핑한다.

권장 UseCaseImpl 형태:

```java
public ClaimSearchResponse searchClaim(ClaimSearchRequest request) {
    CLCNNB00001_I mciRequest = converter.toMciRequest(request);
    CLCNNB00001_O mciResponse = mciClient.call(mciRequest);
    return converter.toResponse(mciResponse);
}
```

## 10. HTTP Tool 구현 절차

1. HTTP API Name, domain, url, method, content type을 확정한다.
2. HTTP 요청·응답 DTO를 작성한다.
3. `{HttpApiName}Client`를 작성한다.
4. Converter에서 Tool DTO와 HTTP DTO를 변환한다.
5. UseCaseImpl이 Client를 호출하고 응답을 변환한다.
6. 환경별 `application-glow-*.yml`에 API 설정을 반영한다.

```yaml
glow:
  communication:
    http:
      connection-timeout: 5
      read-timeout: 5
      api-list:
        - name: insurance
          domain: ${AXHUB_INSURANCE_HTTP_DOMAIN:http://localhost:${server.port}}
          url: ${AXHUB_INSURANCE_HTTP_URL:/api/mock/http/ins_insurance_processor}
          method: POST
          content-type: application/json;charset=UTF-8
          biz-pod: false
```

`domain + url`이 최종 호출 주소가 된다. `name`은 Java Client의 `API_NAME`과 반드시 일치해야 한다.

## 11. V17 메타데이터 작성

`tool-definitions/{categoryKey}/{toolName}.yml`에서 다음 항목을 업무 기준으로 수정한다.

- `display_name`
- `description.function`
- `description.when_to_use`
- `description.when_not_to_use`
- `description.io_limits`
- `display_description`
- `example_queries` 3~10개
- `read_only`, `destructive`, `idempotent`
- `parameters_schema`
- `tags`
- `legacy_interface_id`
- `required_env_keys`
- `owner_org`

AI 자동 채움 결과는 초안으로만 사용하고, Tool 개발자가 인터페이스 정의서와 실제 소스를 기준으로 검수한다.

## 12. Input/Output Schema 적용

### 단순 Tool

- Request 필드에 `@Schema` 또는 `@McpToolParam`
- Response 클래스에 `@McpOutputSchema`
- DTO 기반 자동 Schema 생성

### 복잡한 Tool

- `tool-schemas/{categoryKey}`에 JSON 파일 작성
- `@GrowToolHint.inputSchemaResource` 지정
- `@GrowToolHint.outputSchemaResource` 지정
- 조건부 필드, 배열 요소, null 허용 여부까지 명시

JSON Schema에서 null을 허용하려면 타입 규칙에 명시해야 한다. Java 응답이 null을 반환하는데 Schema가 `string`이나 `integer`만 허용하면 실행 결과 검증에서 실패한다.

## 13. 로컬 Mock 테스트

local 프로파일에서는 공통 Mock HTTP endpoint를 사용할 수 있다.

```text
POST /api/mock/http/{toolName}
```

Mock 응답 파일은 다음 경로에서 Tool명과 맞춰 관리한다.

```text
src/main/resources/mock-responses/{toolName}.json
```

파일명이 다르거나 classpath에 포함되지 않으면 `Not found in blob store`와 같은 오류가 발생할 수 있다. 파일명, 대소문자, 리소스 경로를 확인한다.

## 14. Tool 테스트 콘솔

Tool Pod 실행 후 다음 주소로 접속한다.

```text
http://localhost:{podPort}/tool-test-console.html
```

테스트 순서:

1. Tool 목록 새로고침
2. 대상 Tool 선택
3. Schema 기반 샘플 요청 확인
4. 정상 요청 실행
5. 필수값 누락·형식 오류·경계값 실행
6. 응답 결과와 Output Schema 검증 확인
7. `trace-id`, `request-id` 및 처리 시간 확인
8. 테스트 케이스 저장 후 회귀 테스트

## 15. 자동 검증 명령

```powershell
# 공통 단위 테스트
.\gradlew.bat :dat-was-lib:test

# 대상 Pod 테스트
.\gradlew.bat :dat-was-cus:test

# Tool명 중복
.\gradlew.bat validateMcpToolNames

# Tool Schema V17
.\gradlew.bat validateToolSchemaV17

# 배포 산출물 생성: 위 두 검증이 자동 선행됨
.\gradlew.bat :dat-was-cus:bootJar
```

Docker는 컴파일과 일반 단위 테스트에 필요하지 않다. 실제 HTTP/MCI Mock 컨테이너를 사용하는 연계 테스트에서만 실행한다.

## 16. 장애 확인 순서

### Tool이 목록에 없을 때

1. UseCase 메서드에 `@McpTool`이 있는지 확인한다.
2. UseCase 구현체가 Spring Bean인지 확인한다.
3. `@GrowToolHint.register` 값을 확인한다.
4. Tool명과 V17 정의 파일의 `name`이 같은지 확인한다.
5. 애플리케이션의 component scan 범위를 확인한다.

### HTTP 호출이 실패할 때

1. Client의 `API_NAME`을 확인한다.
2. `glow.communication.http.api-list[].name`과 비교한다.
3. `domain + url`을 확인한다.
4. method와 content-type을 확인한다.
5. timeout, 인증서, DNS, ACL을 확인한다.

### MCI 호출이 실패할 때

1. 인터페이스 ID와 수신 서비스 ID를 확인한다.
2. host, port, uri를 확인한다.
3. 요청 전문 필드와 인코딩을 확인한다.
4. 공통 헤더와 세션성 값의 필요 여부를 확인한다.
5. MCI 응답 헤더의 결과 코드와 메시지를 확인한다.

### Output Schema 검증이 실패할 때

1. 실제 응답 JSON을 확인한다.
2. null 허용 여부를 확인한다.
3. integer/string/array/object 타입을 비교한다.
4. required 필드가 실제로 항상 존재하는지 확인한다.
5. JSON Schema와 `@McpOutputSchema` 중 어떤 방식이 선택됐는지 확인한다.

## 17. 개발 완료 체크리스트

- [ ] 올바른 Tool Pod와 category를 선택했다.
- [ ] Tool명이 정규식과 업무 명명 규칙을 만족한다.
- [ ] `@McpTool`의 title과 description을 구분해 작성했다.
- [ ] V17 정의 파일을 업무 내용으로 검수했다.
- [ ] Request/Response DTO와 연계 DTO를 분리했다.
- [ ] Converter 테스트를 작성했다.
- [ ] MCI 또는 HTTP Client가 공통 Component를 사용한다.
- [ ] endpoint와 Secret을 코드에 하드코딩하지 않았다.
- [ ] 정상·필수값 누락·타입 오류·경계값을 테스트했다.
- [ ] 응답에 불필요한 개인정보가 없다.
- [ ] Tool명 중복 검사와 V17 검증이 성공한다.
- [ ] 대상 Pod의 단위 테스트와 `bootJar`가 성공한다.
- [ ] Tool 테스트 콘솔에서 실제 실행 결과를 확인했다.

## 18. 1차본 이후 보완 대상

다음 항목은 운영 표준이 확정되면 2차본에 반영한다.

- 업무 도메인별 Pod 배치 기준의 상세화
- 인증·암호화 사번 처리 최종 규격
- 연계 오류 코드의 공통 사용자 메시지 표준
- Tool별 SLA, Retry, Circuit Breaker 기준
- 성능·부하·보안 테스트 기준
- 운영 모니터링과 장애 대응 절차
