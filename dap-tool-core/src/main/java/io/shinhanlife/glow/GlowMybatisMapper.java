package io.shinhanlife.glow;


/**
 * @package io.shinhanlife.glow
 * @className GlowMybatisMapper
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
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * MyBatis Mapper 인터페이스를 나타내는 어노테이션
 * MyBatis Mapper와 동일한 기능을 제공합니다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapper
@Component
public @interface GlowMybatisMapper {
}
