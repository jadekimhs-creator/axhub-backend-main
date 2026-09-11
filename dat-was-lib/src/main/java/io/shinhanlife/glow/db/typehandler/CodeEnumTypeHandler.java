package io.shinhanlife.glow.db.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * glow 프레임워크 스텁 (실제 라이브러리가 없는 로컬 개발 환경용 더미).
 * {@link CodeEnum}을 구현하는 코드 enum과 DB 문자열 컬럼(코드값)을 상호 변환하는 MyBatis TypeHandler
 * 공통 베이스. common/enums/type의 {@code {ClassName}TypeHandler}는 모두 이 클래스를 상속하고,
 * 생성자에서 자신의 enum 타입을 super(...)로 넘기기만 한다.
 */
public abstract class CodeEnumTypeHandler<E extends Enum<E> & CodeEnum> extends BaseTypeHandler<E> {

    private final Class<E> type;

    protected CodeEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toEnum(rs.getString(columnName));
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toEnum(rs.getString(columnIndex));
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toEnum(cs.getString(columnIndex));
    }

    private E toEnum(String code) {
        if (code == null) {
            return null;
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.getCode().equals(code)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("알 수 없는 코드 [" + code + "] - " + type.getName());
    }
}
