package io.shinhanlife.axhub.biz.mcp.adapter.support;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class DynamicSchemaValidator {
    
    public boolean validate(List<Map<String, Object>> specList, Map<String, Object> data, StringBuilder errorLog) {
        for (Map<String, Object> spec : specList) {
            String name = (String) spec.get("name");
            String type = (String) spec.get("type");
            boolean isRequired = spec.get("required") != null && (Boolean) spec.get("required");
            Object value = data.get(name);

            // 1. 필수값 체크
            if (isRequired && (value == null || String.valueOf(value).trim().isEmpty())) {
                errorLog.append(String.format("[%s] 필드는 필수 입력 항목입니다. ", name));
                return false;
            }

            // 2. 타입 체크 (값이 있을 때만)
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                if ("NUMBER".equalsIgnoreCase(type) && !String.valueOf(value).matches("-?\\d+(\\.\\d+)?")) {
                    errorLog.append(String.format("[%s] 필드는 숫자여야 합니다. ", name));
                    return false;
                }
            }
        }
        return true;
    }
}