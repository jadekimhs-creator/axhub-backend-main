package io.shinhanlife.dap.mcc.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.shinhanlife.dap.mcc.annotation.McpParameter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonSchemaGenerator {

    /**
     * Java DTO 클래스를 분석하여 MCP 규격의 완전한 JSON Schema를 생성합니다.
     */
    public static Map<String, Object> generateSchema(Class<?> clazz) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        List<String> requiredList = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            Map<String, Object> fieldSchema = new HashMap<>();
            
            // 1. 타입 매핑
            fieldSchema.put("type", mapJavaTypeToJsonType(field.getType()));
            
            // 2. 어노테이션 기반 설명 추출
            McpParameter paramAnnotation = field.getAnnotation(McpParameter.class);
            JsonPropertyDescription descAnnotation = field.getAnnotation(JsonPropertyDescription.class);
            if (paramAnnotation != null && !paramAnnotation.description().isEmpty()) {
                fieldSchema.put("description", paramAnnotation.description());
            } else if (descAnnotation != null && !descAnnotation.value().isEmpty()) {
                fieldSchema.put("description", descAnnotation.value());
            } else {
                fieldSchema.put("description", field.getName()); // 기본값
            }
            
            // 3. 필수 여부 판단
            JsonProperty jsonProp = field.getAnnotation(JsonProperty.class);
            if ((jsonProp != null && jsonProp.required()) || (paramAnnotation != null && paramAnnotation.required())) {
                requiredList.add(field.getName());
            }
            
            properties.put(field.getName(), fieldSchema);
        }

        schema.put("properties", properties);
        if (!requiredList.isEmpty()) {
            schema.put("required", requiredList);
        }

        return schema;
    }

    private static String mapJavaTypeToJsonType(Class<?> clazz) {
        if (clazz == String.class) return "string";
        if (clazz == Integer.class || clazz == int.class) return "integer";
        if (clazz == Long.class || clazz == long.class) return "integer";
        if (clazz == Double.class || clazz == double.class) return "number";
        if (clazz == Float.class || clazz == float.class) return "number";
        if (clazz == Boolean.class || clazz == boolean.class) return "boolean";
        if (List.class.isAssignableFrom(clazz)) return "array";
        return "object";
    }
}