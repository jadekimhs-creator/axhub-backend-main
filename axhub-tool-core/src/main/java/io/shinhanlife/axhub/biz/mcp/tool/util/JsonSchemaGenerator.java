package io.shinhanlife.axhub.biz.mcp.tool.util;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.util
 * @className JsonSchemaGenerator
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
public class JsonSchemaGenerator {

    /**
     * Java DTO 클래스를 분석하여 MCP 규격의 JSON Schema (Properties)를 생성합니다.
     */
    public static Map<String, Object> generatePropertiesSchema(Class<?> clazz) {
        Map<String, Object> properties = new HashMap<>();

        for (Field field : clazz.getDeclaredFields()) {
            Map<String, Object> fieldSchema = new HashMap<>();
            
            // 1. 타입 매핑
            fieldSchema.put("type", mapJavaTypeToJsonType(field.getType()));
            
            // 2. 어노테이션 기반 설명 추출
            McpParameter paramAnnotation = field.getAnnotation(McpParameter.class);
            if (paramAnnotation != null && !paramAnnotation.description().isEmpty()) {
                fieldSchema.put("description", paramAnnotation.description());
            } else {
                fieldSchema.put("description", field.getName()); // 기본값
            }
            
            properties.put(field.getName(), fieldSchema);
        }

        return properties;
    }

    private static String mapJavaTypeToJsonType(Class<?> clazz) {
        if (clazz == String.class) return "string";
        if (clazz == Integer.class || clazz == int.class) return "integer";
        if (clazz == Long.class || clazz == long.class) return "integer";
        if (clazz == Double.class || clazz == double.class) return "number";
        if (clazz == Float.class || clazz == float.class) return "number";
        if (clazz == Boolean.class || clazz == boolean.class) return "boolean";
        if (java.util.List.class.isAssignableFrom(clazz)) return "array";
        return "object";
    }
}