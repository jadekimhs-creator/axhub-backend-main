package io.shinhanlife.glow.util;

import io.shinhanlife.glow.GlowMciFieldInfo;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @package io.shinhanlife.glow.util
 * @className GlowMciParser
 * @description MCI 고정 길이 전문 파싱 유틸리티 (GlowMciFieldInfo 기반)
 * @author 0986406
 * @create 2026.09.01
 */
@Slf4j
public class GlowMciParser {

    public static <T> T parse(String mciString, Class<T> clazz) {
        if (mciString == null || mciString.isEmpty()) {
            return null;
        }

        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            
            List<Field> fields = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(GlowMciFieldInfo.class)) {
                    fields.add(field);
                }
            }

            fields.sort(Comparator.comparingInt(f -> f.getAnnotation(GlowMciFieldInfo.class).order()));

            int currentIndex = 0;
            for (Field field : fields) {
                GlowMciFieldInfo annotation = field.getAnnotation(GlowMciFieldInfo.class);
                int length = annotation.length();

                if (currentIndex >= mciString.length()) {
                    break;
                }
                
                int endIndex = Math.min(currentIndex + length, mciString.length());
                String value = mciString.substring(currentIndex, endIndex);

                field.setAccessible(true);
                setFieldValue(instance, field, value, annotation);

                currentIndex += length;
            }

            return instance;
        } catch (Exception e) {
            log.error("GlowMciFieldInfo 파싱 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("MCI 전문 파싱 오류", e);
        }
    }

    private static void setFieldValue(Object instance, Field field, String value, GlowMciFieldInfo annotation) throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        String trimmedValue = value.trim();

        if (fieldType == String.class) {
            field.set(instance, trimmedValue);
        } else if (fieldType == int.class || fieldType == Integer.class) {
            field.set(instance, trimmedValue.isEmpty() ? 0 : Integer.parseInt(trimmedValue));
        } else if (fieldType == long.class || fieldType == Long.class) {
            field.set(instance, trimmedValue.isEmpty() ? 0L : Long.parseLong(trimmedValue));
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            field.set(instance, Boolean.parseBoolean(trimmedValue));
        } else if (fieldType == double.class || fieldType == Double.class) {
            field.set(instance, trimmedValue.isEmpty() ? 0.0 : Double.parseDouble(trimmedValue));
        } else if (List.class.isAssignableFrom(fieldType)) {
            Class<?> targetClass = annotation.target();
            if (targetClass != void.class) {
                List<Object> list = new ArrayList<>();
                int itemLength = calculateTotalLength(targetClass);
                if (itemLength > 0) {
                    for (int i = 0; i < value.length(); i += itemLength) {
                        int end = Math.min(i + itemLength, value.length());
                        String itemStr = value.substring(i, end);
                        if (itemStr.trim().isEmpty()) continue;
                        list.add(parse(itemStr, targetClass));
                    }
                }
                field.set(instance, list);
            }
        } else {
            field.set(instance, trimmedValue);
        }
    }

    private static int calculateTotalLength(Class<?> clazz) {
        int total = 0;
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(GlowMciFieldInfo.class)) {
                total += field.getAnnotation(GlowMciFieldInfo.class).length();
            }
        }
        return total;
    }
}
