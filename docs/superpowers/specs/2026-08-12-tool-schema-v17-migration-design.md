# Tool Schema V17 일괄 전환 설계

## 1. 목표

BC-DAB-STD-003 V17의 도구 스키마 요구사항을 현재 Java Tool Pod 구조에 일괄 적용한다. 표준 메타데이터를 단일 원본으로 관리하고, 동일 정보가 MCP Tool 정의, Tool Manifest, Gateway Registry, Portal 검색, Scaffold 및 빌드 검증에 일관되게 사용되도록 한다.

기존 Tool 호출명보다 V17 표준 준수를 우선한다. 이름이 변경되는 Tool은 Agent와 Portal 등록도 함께 갱신해야 한다.

## 2. 표준 원본

각 Tool은 모듈의 `src/main/resources/tool-definitions/{category}/{tool-name}.yml`에 표준 정의 파일을 가진다.

필수 항목은 다음과 같다.

- `name`: `^[a-z][a-z0-9_]{2,63}$`
- `display_name`
- `version`
- `category_key`
- `description.function`
- `description.when_to_use`
- `description.when_not_to_use`
- `description.io_limits`
- `display_description`
- `example_queries`: 실제 사용자 발화 3~10건
- `read_only`
- `destructive`
- `idempotent`
- `parameters_schema`

권장·조건부 항목은 `tags`, `legacy_interface_id`, `required_env_keys`, `owner_org`, `output_schema`로 한다.

Java `@McpTool`은 실행 메서드를 식별하는 용도로 유지한다. 등록 시 표준 정의 파일을 우선 적용하고, 정의 파일이 없는 Tool은 빌드 품질 게이트에서 실패시킨다.

## 3. 런타임 데이터 흐름

1. Tool Pod 기동 시 `@McpTool` 메서드를 스캔한다.
2. Tool 이름으로 표준 정의 파일을 읽는다.
3. annotation의 이름과 표준 정의의 이름이 다르면 기동 실패한다.
4. 입력 Schema는 기존 우선순위를 유지한다.
   - `ToolHint.inputSchemaResource`
   - 표준 정의의 `parameters_schema`
   - DTO 자동 생성
5. 출력 Schema는 다음 우선순위를 사용한다.
   - `ToolHint.outputSchemaResource`
   - 표준 정의의 `output_schema`
   - `@McpOutputSchema` DTO 자동 생성
6. Tool Metadata와 Manifest를 생성한다.
7. Tool Pod MCP 서버와 Gateway MCP 서버에 동일한 title, description, inputSchema, outputSchema, annotations, `_meta`를 등록한다.

모델에 전달되는 `description`은 설명 4요소를 순서대로 결합한다. `example_queries`, 운영 조직, 기간계 ID와 버전은 `_meta`에 두어 모델 프롬프트 토큰을 늘리지 않는다.

## 4. 메타데이터 및 Manifest 확장

`ToolMetadata`에 다음 값을 추가한다.

- `displayDescription`
- `descriptionFunction`
- `whenToUse`
- `whenNotToUse`
- `ioLimits`
- `exampleQueries`
- `tags`
- `legacyInterfaceId`
- `requiredEnvKeys`
- `ownerOrg`
- `outputSchema`

Manifest Tool 항목에는 `outputSchema`를 추가한다. `_meta`에는 `version`, `categoryKey`, `exampleQueries`, `tags`, `legacyInterfaceId`, `requiredEnvKeys`, `ownerOrg`, `timeoutMillis`, `enabled`를 제공한다.

기존 `mciServiceId`는 호환을 위해 유지하되, 표준 Manifest에서는 `legacyInterfaceId`로 노출한다.

## 5. MCP 매핑

- `name` -> `Tool.name`
- `display_name` -> `Tool.title` 및 `annotations.title`
- 설명 4요소 -> `Tool.description`
- `parameters_schema` -> `Tool.inputSchema`
- `output_schema` -> `Tool.outputSchema`
- `read_only` -> `readOnlyHint`
- `destructive` -> `destructiveHint`
- `idempotent` -> `idempotentHint`
- 폐쇄망 Tool의 `openWorldHint` -> `false`
- 예시 질의와 운영 메타 -> `Tool._meta` 및 Manifest `_meta`

행위 힌트는 인가 수단으로 사용하지 않는다. 기존 Gateway/Tool 권한 검증이 실제 접근을 차단한다.

## 6. Tool 이름 전환

모든 이름을 영문 소문자 snake_case 3~64자로 통일한다. 현재 확인된 변경 대상은 다음과 같다.

- `smp_exchangeRate_inquiry` -> `smp_exchange_inquiry`
- `cmm_commonCode_lookup` -> `cmm_comcode_lookup`

나머지 Tool도 같은 정규식과 `category_service_action` 의미 구조로 검증한다. MCI/EIMS 인터페이스 ID는 Tool명으로 사용하지 않고 `legacy_interface_id`에 저장한다.

## 7. 설명과 유사 Tool 경계

현재 모든 Tool에 설명 4요소를 작성한다. `when_not_to_use`는 유사 Tool이 있으면 실제 Tool명을 양방향으로 기재한다. 유사 Tool이 없으면 `없음`을 명시한다.

자기 홍보 문구와 강제 선택 문구는 금지한다. 예시 질의에는 Tool 이름을 포함하지 않는다.

## 8. 품질 게이트

Gradle의 기존 Tool 이름 검증을 V17 검증으로 확장한다. `check`와 `bootJar`가 V17 검증에 의존하도록 한다.

다음을 위반하면 파일과 항목을 표시하고 빌드를 실패시킨다.

- 필수 14개 항목 누락
- 이름 정규식 위반 또는 중복
- annotation 이름과 정의 파일 이름 불일치
- 설명 4요소 누락
- 예시 질의 3~10건 위반 또는 Tool명 포함
- 자기 홍보 문구 포함
- 입력 필드 description 누락
- `additionalProperties: false` 미설정
- `read_only=true`와 `destructive=true` 동시 설정
- 등록 Tool의 정의 파일 누락 또는 사용되지 않는 정의 파일 존재
- outputSchema가 선언된 경우 유효하지 않은 구조

플랫폼이 주입하는 trace ID, request ID, employee ID 및 인증 값은 Tool 입력 Schema에 포함하지 않는다.

## 9. Scaffold

Scaffold 화면과 생성기는 다음 값을 받는다.

- Tool명 또는 Base Name
- 표시명과 화면 설명
- 설명 4요소
- 예시 질의 3~10건
- category, tags, 담당 조직, 기간계 ID
- 읽기 전용·파괴적·멱등 힌트
- 입력·출력 필드와 Schema 제약

생성 결과에 Java UseCase/DTO/Converter/Client, Tool 정의 YAML, 필요 시 JSON Schema Resource, 테스트 및 Mock 응답을 포함한다. 생성 직후 V17 검증을 실행하고 결과를 화면에 표시한다.

## 10. 기존 Tool 일괄 마이그레이션

SMS와 OTH 모듈의 모든 `@McpTool`을 대상으로 정의 파일을 생성한다. 기존 title, description, DTO, 연동 코드와 테스트를 참고하여 필드를 작성한다. 정보가 소스에서 확정되지 않는 경우 다음 보수적 기본값을 사용한다.

- `owner_org`: `MCP_TOOL`
- 유사 Tool 없음: `when_not_to_use: 없음`
- 내부 연동: `openWorldHint: false`
- 조회성 이름·구현: 읽기 전용 true, 파괴적 false, 멱등 true
- 등록·발송·처리성 구현: 읽기 전용 false, 파괴적 true, 멱등 false

`register=false`는 초안/로컬 Tool 상태로 유지한다. 중앙 Registry에 올릴 Tool만 승인 후 `register=true`로 전환하며, 이번 변경에서 임의로 운영 등록을 활성화하지 않는다.

사용자가 별도로 작업 중인 미추적 WCM/HMCI 파일은 덮어쓰지 않는다. 스캔된 WCM Tool용 정의 파일과 검증 지원만 추가한다.

## 11. 호환성과 영향

이름이 바뀐 Tool은 기존 호출명으로 호출할 수 없다. Portal, DeepAgentBuilder, Agent 설정, 테스트 데이터 및 Mock mapping에서 이름을 함께 변경한다.

MCP SSE/Streamable HTTP 전송 경로, MCI/HTTP 연동 로직, trace/request ID 흐름은 변경하지 않는다. 이번 범위는 Tool 정의·등록·검색용 메타데이터와 검증에 한정한다.

기존 JSON input/output Schema Resource 방식은 유지한다. 복잡한 Tool은 Resource를 사용하고 단순 Tool은 DTO 기반 자동 생성을 계속 사용할 수 있다.

## 12. 테스트 기준

- 표준 정의 로더 단위 테스트
- 필수 필드 및 반려 조건별 검증 테스트
- description 렌더링과 `_meta` 매핑 테스트
- input/output Schema 우선순위 테스트
- Tool Manifest 직렬화 테스트
- Tool Pod 및 Gateway MCP Tool 정의 동등성 테스트
- Scaffold 생성물 컴파일 및 V17 검증 테스트
- 전체 모듈 컴파일·테스트
- 전체 Tool 정의 V17 검증 통과

완료 기준은 모든 등록 대상 Tool이 정의 파일을 보유하고, `check`와 `bootJar`에서 V17 검증을 통과하며, MCP `tools/list`와 Manifest에 동일한 표준 정보가 노출되는 것이다.
