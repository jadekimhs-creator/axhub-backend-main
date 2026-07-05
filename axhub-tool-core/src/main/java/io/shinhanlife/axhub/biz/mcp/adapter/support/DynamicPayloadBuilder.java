package io.shinhanlife.axhub.biz.mcp.adapter.support;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DynamicPayloadBuilder {

    public String buildFixedLengthString(List<Map<String, Object>> specList, Map<String, Object> data) throws Exception {
        StringBuilder sb = new StringBuilder();

        for (Map<String, Object> spec : specList) {
            String name = (String) spec.get("name");
            // EIMS가 고정장을 요구할 경우를 대비한 len 파라미터 체크 (기본값 0 방어)
            int len = spec.get("len") != null ? (Integer) spec.get("len") : 0;
            String type = (String) spec.get("type");
            String rawValue = String.valueOf(data.getOrDefault(name, ""));

            // len 값이 없으면 변환 없이 바로 이어붙임 (JSON 통신용)
            if (len == 0) {
                sb.append(rawValue);
                continue;
            }

            // len 값이 있으면 고정장 통신 규칙 적용
            byte[] rawBytes = rawValue.getBytes("EUC-KR");
            if (rawBytes.length > len) {
                throw new IllegalArgumentException(name + " 길이가 " + len + " 바이트를 초과합니다.");
            }

            int padLength = len - rawBytes.length;
            StringBuilder paddedValue = new StringBuilder(rawValue);

            if ("NUMBER".equalsIgnoreCase(type)) {
                for (int i = 0; i < padLength; i++) paddedValue.insert(0, "0");
            } else {
                for (int i = 0; i < padLength; i++) paddedValue.append(" ");
            }
            sb.append(paddedValue.toString());
        }
        return sb.toString();
    }
}