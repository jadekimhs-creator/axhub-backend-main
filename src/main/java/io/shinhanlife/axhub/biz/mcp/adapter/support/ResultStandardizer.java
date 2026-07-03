package io.shinhanlife.axhub.biz.mcp.adapter.support;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ResultStandardizer {

    // 오라클 조회 결과(Map)의 키 값을 카멜 케이스로 변환 (스키마 매핑)
    public Map<String, Object> standardize(Map<String, Object> rawData) {
        Map<String, Object> standardMap = new HashMap<>();

        rawData.forEach((key, value) -> {
            String camelKey = convertToCamelCase(key);
            standardMap.put(camelKey, value);
        });

        return standardMap;
    }

    private String convertToCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) return snakeCase;

        StringBuilder result = new StringBuilder();
        boolean nextIsUpper = false;

        for (char c : snakeCase.toLowerCase().toCharArray()) {
            if (c == '_') {
                nextIsUpper = true;
            } else {
                result.append(nextIsUpper ? Character.toUpperCase(c) : c);
                nextIsUpper = false;
            }
        }
        return result.toString();
    }
}