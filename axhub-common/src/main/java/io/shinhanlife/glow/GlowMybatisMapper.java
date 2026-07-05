package io.shinhanlife.glow;

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
