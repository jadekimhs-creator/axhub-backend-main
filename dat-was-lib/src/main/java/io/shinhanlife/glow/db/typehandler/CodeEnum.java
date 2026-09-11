package io.shinhanlife.glow.db.typehandler;

/**
 * glow 프레임워크 스텁 (실제 라이브러리가 없는 로컬 개발 환경용 더미).
 * 코드값을 갖는 enum이 공통으로 구현하는 인터페이스 — {@link CodeEnumTypeHandler}가 이 getCode()로
 * DB 컬럼(String)과 enum 상수를 상호 변환한다.
 */
public interface CodeEnum {

    String getCode();
}
