package io.shinhanlife.glow.util;

import io.shinhanlife.glow.communication.annotation.GlowTrgmField;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @package io.shinhanlife.glow.util
 * @className GlowTrgmParser
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
@Slf4j
public class GlowTrgmParser {

    /**
     * 고정 길이 문자열 전문을 @GlowTrgmField 어노테이션 정보에 따라 DTO 객체로 파싱합니다.
     * @param trgmString 원본 전문 문자열
     * @param clazz 매핑할 DTO 클래스
     * @return 파싱된 DTO 인스턴스
     */
    public static <T> T parse(String trgmString, Class<T> clazz) {
        if (trgmString == null || trgmString.isEmpty()) {
            return null;
        }

        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            
            List<Field> fields = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(GlowTrgmField.class)) {
                    fields.add(field);
                }
            }

            fields.sort(Comparator.comparingInt(f -> f.getAnnotation(GlowTrgmField.class).order()));

            int currentIndex = 0;
            for (Field field : fields) {
                GlowTrgmField annotation = field.getAnnotation(GlowTrgmField.class);
                int length = annotation.length();

                if (currentIndex >= trgmString.length()) {
                    break;
                }
                
                int endIndex = Math.min(currentIndex + length, trgmString.length());
                String value = trgmString.substring(currentIndex, endIndex).trim();

                field.setAccessible(true);
                setFieldValue(instance, field, value);

                currentIndex += length;
            }

            return instance;
        } catch (Exception e) {
            log.error("GlowTrgmField 파싱 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("전문 파싱 오류", e);
        }
    }

    private static void setFieldValue(Object instance, Field field, String value) throws IllegalAccessException {
        Class<?> fieldType = field.getType();

        if (fieldType == String.class) {
            field.set(instance, value);
        } else if (fieldType == int.class || fieldType == Integer.class) {
            field.set(instance, value.isEmpty() ? 0 : Integer.parseInt(value));
        } else if (fieldType == long.class || fieldType == Long.class) {
            field.set(instance, value.isEmpty() ? 0L : Long.parseLong(value));
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            field.set(instance, Boolean.parseBoolean(value));
        } else if (fieldType == double.class || fieldType == Double.class) {
            field.set(instance, value.isEmpty() ? 0.0 : Double.parseDouble(value));
        } else {
            field.set(instance, value);
        }
    }
}
