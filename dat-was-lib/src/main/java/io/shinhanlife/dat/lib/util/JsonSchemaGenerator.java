package io.shinhanlife.dat.lib.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springaicommunity.mcp.annotation.McpToolParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @package io.shinhanlife.dat.lib.util
 * @className JsonSchemaGenerator
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
public class JsonSchemaGenerator {

    /**
     * Java DTO 클래스를 분석하여 MCP 규격의 완전한 JSON Schema를 생성합니다.
     */
    public static Map<String, Object> generateSchema(Class<?> clazz) {
        return generateSchema(clazz, new HashSet<>());
    }

    private static Map<String, Object> generateSchema(Class<?> clazz, Set<Class<?>> visiting) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        if (!visiting.add(clazz)) {
            return schema;
        }
        
        Map<String, Object> properties = new HashMap<>();
        List<String> requiredList = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            Map<String, Object> fieldSchema = createFieldSchema(field, visiting);
            
            // 1. 타입 매핑
            
            // 2. 어노테이션 기반 설명 추출
            McpToolParam paramAnnotation = field.getAnnotation(McpToolParam.class);
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

            Schema schemaAnnotation = field.getAnnotation(Schema.class);
            if (schemaAnnotation != null) {
                if (!schemaAnnotation.description().isEmpty() && !fieldSchema.containsKey("description")) {
                    fieldSchema.put("description", schemaAnnotation.description());
                }
                if ((schemaAnnotation.required() || schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED)
                        && !requiredList.contains(field.getName())) {
                    requiredList.add(field.getName());
                }
                if (!schemaAnnotation.pattern().isEmpty()) {
                    fieldSchema.put("pattern", schemaAnnotation.pattern());
                }
                if (!schemaAnnotation.minimum().isEmpty()) {
                    try {
                        fieldSchema.put("minimum", Long.valueOf(schemaAnnotation.minimum()));
                    } catch (NumberFormatException ignored) {}
                }
                if (!schemaAnnotation.maximum().isEmpty()) {
                    try {
                        fieldSchema.put("maximum", Long.valueOf(schemaAnnotation.maximum()));
                    } catch (NumberFormatException ignored) {}
                }
                if (schemaAnnotation.minLength() > 0) {
                    fieldSchema.put("minLength", schemaAnnotation.minLength());
                }
                if (schemaAnnotation.maxLength() > 0 && schemaAnnotation.maxLength() != Integer.MAX_VALUE) {
                    fieldSchema.put("maxLength", schemaAnnotation.maxLength());
                }
                if (schemaAnnotation.allowableValues().length > 0 && !schemaAnnotation.allowableValues()[0].isEmpty()) {
                    fieldSchema.put("enum", List.of(schemaAnnotation.allowableValues()));
                }
                if (!schemaAnnotation.format().isEmpty()) {
                    fieldSchema.put("format", schemaAnnotation.format());
                }
                if (!schemaAnnotation.defaultValue().isEmpty()) {
                    fieldSchema.put("default", coerceDefaultValue(schemaAnnotation.defaultValue(), field.getType()));
                }
                if (!schemaAnnotation.example().isEmpty()) {
                    fieldSchema.put("examples", List.of(schemaAnnotation.example()));
                }
                if (schemaAnnotation.nullable()) {
                    Map<String, Object> nonNullSchema = new HashMap<>(fieldSchema);
                    fieldSchema = new HashMap<>();
                    fieldSchema.put("anyOf", List.of(
                            nonNullSchema,
                            Map.of("type", "null")
                    ));
                }
            }
            properties.put(field.getName(), fieldSchema);
        }

        schema.put("properties", properties);
        if (!requiredList.isEmpty()) {
            schema.put("required", requiredList);
        }

        // anyOf removed

        visiting.remove(clazz);
        return schema;
    }


    private static Object coerceDefaultValue(String value, Class<?> fieldType) {
        try {
            if (fieldType == Integer.class || fieldType == int.class
                    || fieldType == Long.class || fieldType == long.class
                    || fieldType == Short.class || fieldType == short.class
                    || fieldType == Byte.class || fieldType == byte.class) {
                return Long.valueOf(value);
            }
            if (fieldType == Double.class || fieldType == double.class
                    || fieldType == Float.class || fieldType == float.class) {
                return Double.valueOf(value);
            }
            if (fieldType == Boolean.class || fieldType == boolean.class) {
                return Boolean.valueOf(value);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid MCP default value: " + value, e);
        }
    }
    private static Map<String, Object> createFieldSchema(Field field, Set<Class<?>> visiting) {
        Class<?> fieldType = field.getType();
        if (isSimpleType(fieldType)) {
            return new HashMap<>(Map.of("type", mapJavaTypeToJsonType(fieldType)));
        }
        if (List.class.isAssignableFrom(fieldType)) {
            Map<String, Object> fieldSchema = new HashMap<>();
            fieldSchema.put("type", "array");
            fieldSchema.put("items", generateItemsSchema(field, visiting));
            return fieldSchema;
        }
        return generateSchema(fieldType, visiting);
    }

    private static Map<String, Object> generateItemsSchema(Field field, Set<Class<?>> visiting) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type itemType = parameterizedType.getActualTypeArguments()[0];
            if (itemType instanceof Class<?> itemClass) {
                if (isSimpleType(itemClass)) {
                    return new HashMap<>(Map.of("type", mapJavaTypeToJsonType(itemClass)));
                }
                return generateSchema(itemClass, visiting);
            }
        }
        return new HashMap<>(Map.of("type", "object"));
    }

    private static boolean isSimpleType(Class<?> clazz) {
        return clazz == String.class
                || clazz == Integer.class || clazz == int.class
                || clazz == Long.class || clazz == long.class
                || clazz == Double.class || clazz == double.class
                || clazz == Float.class || clazz == float.class
                || clazz == Boolean.class || clazz == boolean.class;
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
