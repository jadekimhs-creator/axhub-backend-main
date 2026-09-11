# AX HUB Tool 설계 가이드

## 1. 설계 목표

Tool은 AI가 기능을 정확히 선택하고, 입력값을 안전하게 전달하며, 레거시 응답을 사람이 이해할 수 있는 형태로 반환하도록 설계한다.

핵심 원칙은 다음과 같다.

1. Tool의 업무 목적과 호출 조건을 명확히 작성한다.
2. AI용 DTO와 MCI·HTTP 전문 DTO를 분리한다.
3. 입력과 출력의 JSON Schema를 구체적으로 정의한다.
4. endpoint, timeout, 인증 값은 설정으로 분리한다.
5. 동일한 Tool명은 빌드 단계에서 차단한다.

## 2. Tool명 규칙

현재 V17 검증 규칙은 다음과 같다.

```regex
^[a-z][a-z0-9_]{2,63}$
```

권장 형식은 `{업무도메인}_{업무기능}_{행위}`이다.

```text
cmm_claim_search
cmm_comcode_lookup
smp_exchange_inquiry
ins_insurance_processor
```

설계 규칙:

- 소문자 영문, 숫자, 언더스코어만 사용한다.
- Pod명(`cus`, `sal`, `pro`, `sys`)은 Tool명에 넣지 않는다.
- 마침표와 공백을 사용하지 않는다.
- 구현 클래스명이나 인터페이스 ID만으로 이름을 만들지 않는다.
- 이름은 한 번 배포한 뒤 호환성을 위해 가급적 변경하지 않는다.

`mcp.namespace`를 설정하면 런타임 등록명 앞에 namespace가 붙을 수 있으므로, 기본값인 빈 문자열 사용 여부를 Pod 운영 기준과 함께 확정한다.

## 3. 업무 패키지 구조

```text
io.shinhanlife.dat.mcc
├─ biz.{category}
│  ├─ dto
│  ├─ converter
│  └─ usecase
│     └─ impl
└─ infra.itrf
   ├─ mci.{clientSystemCode}
   │  └─ io
   └─ http.{httpApiName}
      └─ io
```

각 계층의 책임은 다음과 같다.

| 계층 | 책임 |
|---|---|
| `biz.*.dto` | AI가 이해하는 Tool 요청·응답 모델 |
| `biz.*.converter` | Tool DTO와 연계 전문 DTO 변환 |
| `biz.*.usecase` | `@McpTool`로 노출할 업무 계약 |
| `biz.*.usecase.impl` | 변환, Client 호출, 응답 해석 |
| `infra.itrf.mci.*` | MCI 인터페이스별 Client와 요청·응답 전문 |
| `infra.itrf.http.*` | HTTP API별 Client와 요청·응답 전문 |

한 UseCase 인터페이스에는 관련된 Tool 함수를 여러 개 선언할 수 있다. 이때 구현체에도 동일한 메서드를 추가하고, 각 메서드마다 독립된 `@McpTool`과 `@GrowToolHint`를 지정한다. 공통 추상 UseCase 상속은 필수가 아니다.

## 4. Tool 선언

```java
@McpTool(
    name = "cmm_claim_search",
    title = "보험금 청구 상태 조회",
    description = "청구번호 또는 계약번호로 보험금 청구 상태를 조회합니다."
)
@GrowToolHint(
    register = true,
    categoryKey = "cmm",
    mappingId = "CLCNNB00001"
)
ClaimSearchResponse searchClaim(ClaimSearchRequest request);
```

| 항목 | 작성 기준 |
|---|---|
| `name` | 시스템 식별용 영문 Tool명 |
| `title` | 사용자 화면에 표시할 짧은 업무명 |
| `description` | Tool이 실제로 수행하는 기능 설명 |
| `categoryKey` | 업무 분류 코드 |
| `mappingId` | MCI 인터페이스 ID 또는 업무 연계 식별자 |
| `register` | `false`이면 개발 중인 Tool을 외부 노출 대상에서 제외 |

`title`과 `description`은 같은 문장을 반복하지 않는다. `title`은 짧은 명칭, `description`은 대상·조건·결과를 포함한 한두 문장으로 작성한다.

## 5. Tool Schema V17 메타데이터

각 Tool에는 다음 경로의 정의 파일을 둔다.

```text
src/main/resources/tool-definitions/{categoryKey}/{toolName}.yml
```

필수 구성 예시는 다음과 같다.

```yaml
name: cmm_claim_search
display_name: 보험금 청구 상태 조회
version: 1.0.0
category_key: cmm
description:
  function: 청구번호 또는 계약번호를 기준으로 청구 상태와 심사 결과를 조회한다.
  when_to_use: 기존 청구 건의 진행 상태나 지급 결과를 확인할 때 사용한다.
  when_not_to_use: 신규 보험금 청구 접수나 기존 청구 변경에는 사용하지 않는다.
  io_limits: 청구번호 또는 계약번호 중 하나 이상이 필요하며 조회 기능만 제공한다.
display_description: 보험금 청구 상태와 심사 결과를 조회합니다.
example_queries:
  - 내 보험금 청구가 어디까지 진행됐는지 알려줘
  - 계약번호로 최근 청구 상태를 확인해줘
  - 청구 심사 결과가 나왔는지 조회해줘
read_only: true
destructive: false
idempotent: true
parameters_schema:
  type: object
  properties:
    claimNo:
      type: string
      description: 조회할 보험금 청구번호
  additionalProperties: false
tags: [보험금, 청구조회]
legacy_interface_id: CLCNNB00001
required_env_keys: []
owner_org: MCP_TOOL
```

V17 검증 기준:

- `name`은 Tool명 정규식을 만족해야 한다.
- 표시명, 버전, 분류, 4종 설명은 비어 있으면 안 된다.
- 자연어 예시 질의는 3~10개이며 Tool명을 직접 포함하지 않는다.
- `read_only`, `destructive`, `idempotent`를 명시한다.
- Schema 최상위 타입은 `object`이다.
- 모든 속성에 `description`이 있어야 한다.
- `additionalProperties`는 `false`이다.

## 6. Input Schema 설계

### 6.1 단순한 요청

필드 수가 적고 조건이 단순하면 DTO의 `@Schema`, `@McpToolParam`과 타입 정보를 이용해 생성한다.

```java
@Schema(
    description = "보험금 청구번호",
    example = "CLM202608100001",
    requiredMode = Schema.RequiredMode.REQUIRED
)
private String claimNo;
```

### 6.2 복잡한 요청

중첩 객체, 조건부 필드, 정규식, 배열 제약처럼 복잡한 규칙은 JSON 리소스로 관리한다.

```text
src/main/resources/tool-schemas/{categoryKey}/{schema-name}-input-schema.json
```

```java
@GrowToolHint(
    categoryKey = "cmm",
    inputSchemaResource = "classpath:tool-schemas/cmm/claim-search-resource-input-schema.json"
)
```

명시한 `inputSchemaResource`가 있으면 리소스 Schema를 우선 사용한다. 리소스가 없을 때 DTO 기반 Schema를 사용한다.

## 7. Output Schema 설계

단순한 응답 DTO는 클래스에 `@McpOutputSchema`를 붙인다.

```java
@McpOutputSchema
public class ClaimSearchResponse {
    // fields
}
```

복잡한 응답은 다음 리소스를 사용한다.

```text
src/main/resources/tool-schemas/{categoryKey}/{schema-name}-output-schema.json
```

```java
@GrowToolHint(
    categoryKey = "cmm",
    outputSchemaResource = "classpath:tool-schemas/cmm/claim-search-resource-output-schema.json"
)
```

우선순위는 **명시 JSON 리소스 → `@McpOutputSchema` DTO 생성 → 미사용** 순서다. Output Schema를 명시한 Tool만 실행 결과를 해당 Schema로 검증한다.

## 8. 응답 DTO 작성 규칙

1. 코드와 라벨을 함께 제공한다: `status`, `statusLabel`.
2. null의 의미를 필드 설명에 명시한다: “심사 전이면 null이며 0원으로 해석하지 않는다.”
3. 조건부 필드는 조건을 설명한다: “status가 REJECTED일 때만 값이 있다.”
4. 배열에는 정렬 기준을 설명한다: “접수일 내림차순.”
5. 결과가 잘릴 수 있으면 `hasMore` 같은 필드를 제공한다.
6. 민감정보는 가능한 한 응답에 포함하지 않는다. 마스킹은 생략이 불가능할 때의 보조 수단이다.
7. 레거시 원문 응답을 그대로 반환하지 않고 Tool 응답 DTO에 필요한 값만 매핑한다.

배열 요소가 복합 객체이면 응답 DTO 안에 의미 있는 inner class를 정의할 수 있다.

```java
public class ActivityStatusResponse {
    private List<ActivityItem> items;

    public static class ActivityItem {
        private String status;
        private String statusLabel;
    }
}
```

## 9. MCI 연동 설계

권장 호출 흐름은 다음과 같다.

```text
Tool Request
  → UseCaseImpl
  → Converter.toMciRequest()
  → Mci{System}Client
  → AxhubMciComponent
  → GlowMciComponent
  → Converter.toResponse()
  → Tool Response
```

파일 구성 예:

```text
Onnba3011Request.java
Onnba3011Response.java
Onnba3011Converter.java
Onnba3011UseCase.java
Onnba3011UseCaseImpl.java
MciCfpaClient.java
CLCNNB00001_I.java
CLCNNB00001_O.java
```

UseCaseImpl에서 직접 MCI 전문 필드를 하나씩 조립하지 않는다. 변환은 Converter에 두고, MCI Client는 변환이 끝난 전문을 받는다.

## 10. Glow HTTP 연동 설계

권장 호출 흐름은 다음과 같다.

```text
Tool Request
  → UseCaseImpl
  → Converter.toHttpRequest()
  → {HttpApiName}Client
  → AxhubHttpComponent
  → GlowHttpComponent
  → Converter.toResponse()
  → Tool Response
```

Client는 설정의 API 이름만 참조한다.

```java
@Component
@RequiredArgsConstructor
public class InsuranceClient {
    private static final String API_NAME = "insurance";
    private final AxhubHttpComponent http;

    public <I, O> O call(I request, Class<O> responseType) {
        return http.call(API_NAME, request, responseType);
    }
}
```

URL과 HTTP 메서드는 `glow.communication.http.api-list`에 정의한다.

```yaml
glow:
  communication:
    http:
      api-list:
        - name: insurance
          domain: ${AXHUB_INSURANCE_HTTP_DOMAIN:http://localhost:${server.port}}
          url: ${AXHUB_INSURANCE_HTTP_URL:/api/mock/http/ins_insurance_processor}
          method: POST
          content-type: application/json;charset=UTF-8
          biz-pod: false
```

`name`은 Client의 `API_NAME`과 같아야 한다. 업무 Java 코드에는 실제 host, port, path를 하드코딩하지 않는다.

## 11. 공통 헤더와 추적성

Tool 호출 시 다음 헤더는 선택적으로 받을 수 있다.

| 헤더 | 의미 |
|---|---|
| `trace-id` | 전체 업무 흐름 추적 ID. 연속 호출 동안 유지 |
| `request-id` | 개별 요청 ID. HTTP 호출 단위로 새 값 사용 가능 |
| `employee-id` | 암호화된 사번. 현재 필수 아님 |
| `X-Request-Id` | 호환용 요청 ID |

`AxhubHttpComponent`는 현재 요청 Context의 `trace-id`, `request-id`, 암호화 사번을 Glow HTTP 요청 헤더로 전달한다. 민감 헤더와 DTO 원문은 일반 로그에 그대로 남기지 않는다.

## 12. 빌드 시 자동 검증

```powershell
.\gradlew.bat validateMcpToolNames
.\gradlew.bat validateToolSchemaV17
```

- `validateMcpToolNames`: 모든 Tool 모듈의 `@McpTool(name)` 중복 검사
- `validateToolSchemaV17`: `@McpTool`과 V17 정의 파일의 필수 규칙 검사
- 각 Tool Pod의 `bootJar`는 두 검증 Task에 의존하므로 오류가 있으면 배포 JAR이 생성되지 않는다.

## 13. Tool 설계 완료 조건

- [ ] Tool명이 규칙에 맞고 중복되지 않는다.
- [ ] title, description, 사용/비사용 조건이 구체적이다.
- [ ] V17 Tool 정의 파일이 존재한다.
- [ ] AI DTO와 MCI·HTTP 전문 DTO가 분리되어 있다.
- [ ] Converter가 요청과 응답을 담당한다.
- [ ] endpoint와 timeout이 설정으로 분리되어 있다.
- [ ] Input/Output Schema의 단순·복잡 기준을 적용했다.
- [ ] 응답의 코드·라벨, null 의미, 조건, 배열 정렬, 잘림 여부를 설명했다.
- [ ] 불필요한 개인정보를 입출력에서 제거했다.

