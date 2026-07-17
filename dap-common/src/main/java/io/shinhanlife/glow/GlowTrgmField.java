package io.shinhanlife.glow;


/**
 * @package io.shinhanlife.glow
 * @className GlowTrgmField
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
import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlowTrgmField {

    /**
     * 필드 순서
     */
    int order();

    /**
     * 필드 길이
     */
    int length();

    /**
     * 필드 설명
     */
    String description() default "";

}
