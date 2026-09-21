# V17 Tool Function 스키마 재적용 설계

## 목적

Tool Function의 AI 최적화와 Scaffold 생성 결과가 동일한 V17 규칙을 따르도록 한다. 이 규칙은 이미지로 전달된 운영 기준을 구현 가능한 계약으로 정리한 것이다.

현재 대상 Tool은 조회 전용이다. Tool을 선택할지, 선행 조회를 할지, 사용자에게 추가 입력을 물을지는 Agent LLM이 판단한다. 시스템과 Converter는 호출 이후의 기술적 기본값 처리만 담당한다.

## 이름과 Ground Truth

- 공개 MCP Tool 이름은 `업무단위_기능_대상` 3단 구조를 사용한다. 예: `pro_search_fund`, `cus_detail_contract`.
- `categoryKey`가 업무단위를 이미 제공하므로 Java `baseName`에는 중복하지 않는다. 예: `SearchFund`, `DetailContract`.
- 조회 기능어는 `search`(목록 또는 다건)와 `detail`(단건 또는 상세)만 사용한다.
- `title`과 `displayDescription`은 최초 사용자가 입력한 Ground Truth다. AI 최적화는 두 필드를 바꾸지 못한다.
- `displayDescription`은 연계 화면명이며 고정값이다. example query는 이 화면명의 기능 용어를 반드시 반영한다.

## 수정 가능한 메타데이터

AI 최적화와 생성 결과에서 다음 필드는 수정 또는 보완할 수 있다.

- `name`, `description`, `functionDescription`
- `whenToUse`, `whenNotToUse`, `ioLimits`
- `tags`, `exampleQueries`

`description`과 `functionDescription`은 Tool의 기존 기능 의도를 유지해야 한다. 시스템 또는 채널 구분, 필요한 선행 조회 힌트, 호출 후 Converter의 기본값·기본 기간 처리 범위만 보완할 수 있다.

## 메타데이터 규칙

- `whenToUse`는 이 Tool을 선택할 사용자 질문과 상황을 구체적으로 적는다.
- `whenNotToUse`는 목록/상세 Tool의 경계와 수정·신청·해지 등 CUD 요청을 제외한다고 적는다.
- `tags`는 5~8개다: 업무단위 1개, 한글 핵심 태그 2~4개, 영문 보조 태그 2~4개.
- `exampleQueries`는 3~10개다. Ground Truth, 화면 기능, 실제 사용자 업무 행위를 함께 반영한다.
- `inputFields`와 `outputFields`는 각각 최대 10개다. `pageInfo`, `scrPageInfo`는 이름을 바꾸지 않고 optional 시스템 위임 필드로 보존한다.

## 권한 및 실행 안전성

- 이번 V17 재적용 대상은 조회 전용 Tool이다.
- 생성되는 V17 정의는 `read_only: true`, `destructive: false`, `idempotent: true`를 사용한다.
- 수정, 삭제, 생성, 등록 등의 CUD 동작 또는 관련 안내는 생성하지 않는다.
- Tool 선택·순서·선행 조회 여부는 Converter나 시스템이 결정하지 않는다.

## 구현 범위

1. `scaffold.html`: Tool Function 입력 도움말과 V17 최적화 결과 반영을 위 규칙에 맞춘다. title/displayDescription은 AI 결과로 덮어쓰지 않는다.
2. `ScaffoldingController`: V17 최적화 프롬프트와 서버측 정규화·검증을 보강한다. HTTP도 조회 전용 정의로 생성하도록 한다.
3. `ToolScaffolder`: V17 YAML 기본값과 사용자 입력 옵션의 생성 규칙을 동일하게 적용한다.
4. 테스트: AI 결과 보호, 이름 규칙, 태그·예시 수, Read-only 어노테이션, 페이징 필드 보존을 각각 검증한다.

## 비범위

- 이미 생성되어 있는 Tool 정의 파일이나 Java 소스를 일괄 변환하지 않는다.
- 쓰기(CUD) Tool의 별도 승인·감사·변경 이력 정책은 이번 범위에 포함하지 않는다.
- Agent의 실제 Tool 호출 알고리즘을 변경하지 않는다.
