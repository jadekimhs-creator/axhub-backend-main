package io.shinhanlife.glow;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlowLogTarget {

    Target[] value() default {};

/**
 * @package io.shinhanlife.glow
 * @className Target
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
    enum Target {
        FILE, CONSOLE
    }
}