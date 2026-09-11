# AX HUB 프로젝트 개발 규칙 (AGENTS.md)

이 파일은 AI Agent(Antigravity)가 이 프로젝트에서 작업할 때 항상 명심하고 지켜야 할 규칙을 정의하는 파일입니다.
작업 시 유의해야 할 사항이 생기면 언제든지 이 아래에 자유롭게 내용을 추가해 주세요!

##  개발 가이드라인
* 패키지명은 `controller` 대신 `presentation`을 사용합니다.
* MapStruct 사용 시, Spring DI를 활용하여 의존성 주입(`private final Converter converter;`)을 받는 방식을 권장합니다. (단위 테스트 시에는 `@MockBean` 또는 직접 구현체를 주입하여 테스트)

##  명심해야 할 규칙 추가란
* `application.yml` 등 설정 파일 수정 시 한글이 깨지지 않도록 항상 UTF-8 인코딩을 유지하고, 깨진 문자열(`?\uFFFD` 등)이 발생하지 않도록 각별히 주의한다.
* 이모지는 무조건 넣지 않는다
* import 할것 무조건 한다
* Git commit과 push는 사용자의 명시적인 허락(지시) 없이는 절대 수행하지 않는다.
* 자바 만들때는 무조건 아래 내용을 넣는다 
/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.sms
 * @className AxHubToolSmsApplication
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 * 
 * </pre>
 */
* 신한라이프 인터페이스 통신 프로토콜 및 메시지 포맷 가이드 (준버티컬 Tool 대상):
  * 현재 개발 대상은 **대내MCI / EAI (JSON)** 연동으로 한정한다. (대외MCI FixedLength 연동은 범위에서 제외)
  * 통신 노선, 데이터 변환 규격, 시스템별 연계 방식 등은 MCI/EIMS 상에서 관리되므로 코드 레벨에서 식별하거나 분기 처리하지 않는다. (단순 통합 JSON 요청만 수행)
  * 처리계 UI ↔ 처리계 AP 구간: `HTTPS` / `SSV`를 사용하며, FW에서 x-api를 통해 SSV↔DTO 변환을 수행한다.

* 신한라이프 표준 로그 기준 (Logback 설정):
  * **로그 생성 경로:** `/swlog/어플리케이션명(모듈명)/코드명/` (예: `/swlog/dat-gateway/A01/`)
  * **로그 네이밍 규칙:** `${HOSTNAME}_코드명_yyyyMMdd.log` (예: `${HOSTNAME}_A01_20260722.log`)
  * **기본 로그 구분 코드:** 시스템 운영기록 가동기록의 경우 `A01`을 기본으로 사용한다.
  * **호스트명 동적 할당:** Logback 설정 시 `<property name="HOSTNAME" value="${HOSTNAME}" />` 를 선언하여 사용한다.

* 프로젝트 작업 범위 및 툴 연동 규칙:
  * 현재 `axhub-backend-main` 워크스페이스 내에서는 **오직 `dat-gateway` 모듈만 수정**한다.
  * 툴(Tool) 연동 개발 작업은 반드시 별도의 워크스페이스인 **`C:\eGovFrameDev-4.3.1-64bit\workspace-egov\dat-was-datmt`** 폴더에서 수행한다.
