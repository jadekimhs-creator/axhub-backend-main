package io.shinhanlife.dap.common.adapter.util;

import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @package io.shinhanlife.dap.common.adapter.util
 * @className LegacyDataTransformer
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
@Slf4j
public class LegacyDataTransformer {

    /**
     * AI 모델이 보낸 자유로운 형태의 data를 레거시 시스템 규격(spec)에 맞게 강제 변환합니다.
     * 
     * @param data AI가 보낸 파라미터 맵
     * @param spec 레거시 시스템이 요구하는 파라미터 스펙 (name, type, maxLength, defaultValue, required 등)
     * @return 엄격하게 정제된 파라미터 맵
     */
    public static Map<String, Object> transform(Map<String, Object> data, List<Map<String, Object>> spec) {
        if (spec == null || spec.isEmpty()) {
            return data != null ? data : new HashMap<>();
        }

        Map<String, Object> transformedData = new HashMap<>();

        for (Map<String, Object> fieldSpec : spec) {
            String fieldName = (String) fieldSpec.get("name");
            if (fieldName == null) continue;

            Object rawValue = data != null ? data.get(fieldName) : null;
            Object finalValue = rawValue;

            // 1. 기본값(Default Value) 주입
            if (finalValue == null && fieldSpec.containsKey("defaultValue")) {
                finalValue = fieldSpec.get("defaultValue");
                log.debug(" [DataTransformer] '{}' 필드 누락 -> 기본값 '{}' 주입", fieldName, finalValue);
            }

            // 2. 강제 형변환 (Type Coercion)
            String type = (String) fieldSpec.getOrDefault("type", "string");
            if (finalValue != null) {
                if ("string".equalsIgnoreCase(type) && !(finalValue instanceof String)) {
                    finalValue = String.valueOf(finalValue);
                    log.debug(" [DataTransformer] '{}' 필드 강제 String 형변환", fieldName);
                } else if ("number".equalsIgnoreCase(type) && finalValue instanceof String) {
                    try {
                        finalValue = Long.parseLong((String) finalValue);
                        log.debug(" [DataTransformer] '{}' 필드 강제 Number 형변환", fieldName);
                    } catch (NumberFormatException e) {
                        log.warn(" [DataTransformer] '{}' 필드 Number 형변환 실패. 기존 값 유지", fieldName);
                    }
                }
            }

            // 3. 길이 제한 (Truncation / MaxLength)
            if (finalValue instanceof String && fieldSpec.containsKey("maxLength")) {
                int maxLength = (Integer) fieldSpec.get("maxLength");
                String strVal = (String) finalValue;
                if (strVal.length() > maxLength) {
                    finalValue = strVal.substring(0, maxLength);
                    log.warn(" [DataTransformer] '{}' 필드 길이 초과! {}자로 강제 자름 (Truncated)", fieldName, maxLength);
                }
            }

            // 4. 필수값(Required) 누락 체크 (에러를 던지지 않고 빈 문자열 강제 주입하여 레거시 팅김 방지)
            boolean isRequired = (Boolean) fieldSpec.getOrDefault("required", false);
            if (isRequired && finalValue == null) {
                log.error(" [DataTransformer] 필수 필드 '{}' 누락! 강제 공백 주입하여 시스템 장애 방지", fieldName);
                finalValue = "string".equalsIgnoreCase(type) ? "" : 0;
            }

            if (finalValue != null) {
                transformedData.put(fieldName, finalValue);
            }
        }

        return transformedData;
    }
}