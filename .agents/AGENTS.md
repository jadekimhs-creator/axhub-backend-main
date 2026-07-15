# AX HUB 프로젝트 개발 규칙 (AGENTS.md)

이 파일은 AI Agent(Antigravity)가 이 프로젝트에서 작업할 때 항상 명심하고 지켜야 할 규칙을 정의하는 파일입니다.
작업 시 유의해야 할 사항이 생기면 언제든지 이 아래에 자유롭게 내용을 추가해 주세요!

##  개발 가이드라인
* 패키지명은 `controller` 대신 `presentation`을 사용합니다.
* MapStruct 사용 시, 테스트 환경 에러를 방지하기 위해 생성자(`new ...Impl()`) 대신 `Mappers.getMapper(인터페이스명.class)` 방식으로 인스턴스를 가져옵니다.

##  명심해야 할 규칙 추가란
* 이모지는 무조건 넣지 않는다
* import 할것 무조건 한다
* Git commit과 push는 사용자의 명시적인 허락(지시) 없이는 절대 수행하지 않는다.
* 자바 만들때는 무조건 아래 내용을 넣는다 
/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.sms
 * @className AxHubToolSmsApplication
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */






