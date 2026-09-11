# Scaffold AI V17 Metadata Design

## Goal

`AI로 Tool 채우기` 실행 시 Tool 기본 정보와 입출력 필드뿐 아니라 Tool Schema V17 Metadata도 함께 생성하고 화면에 반영한다.

## Design

- `/api/v1/scaffold/tool-draft`의 AI JSON 계약에 `functionDescription`, `displayDescription`, `whenToUse`, `whenNotToUse`, `ioLimits`, `exampleQueries`, `tags`, `ownerOrg`를 추가한다.
- 서버는 문자열을 trim하고 목록 필드는 비어 있는 항목을 제거하여 반환한다.
- `scaffold.html`은 응답받은 V17 값을 같은 이름의 폼 필드에 채운다. 목록은 화면의 기존 입력 규칙에 맞게 줄바꿈 또는 쉼표 문자열로 변환한다.
- Legacy Interface ID와 Client System Code는 기존 방침대로 AI가 생성하지 않는다.

## Error Handling

- V17 필드가 누락된 과거 형식의 AI 응답도 역직렬화할 수 있게 하되, 서버에서 업무 설명을 기반으로 안전한 기본값을 생성한다.
- AI가 빈 배열 또는 공백 항목을 반환하면 기본 예시 질의, 카테고리 태그, 기본 담당 조직을 적용한다.

## Verification

- Mock AI 응답을 사용하는 MVC 테스트로 모든 V17 필드가 API 응답에 포함되는지 검증한다.
- Gateway 테스트와 Tool Schema V17 검증을 실행한다.
