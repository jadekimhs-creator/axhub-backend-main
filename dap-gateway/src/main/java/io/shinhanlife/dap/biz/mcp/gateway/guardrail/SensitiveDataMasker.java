package io.shinhanlife.dap.biz.mcp.gateway.guardrail;


/**
 * @package io.shinhanlife.dap.biz.mcp.gateway.guardrail
 * @className SensitiveDataMasker
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
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Audit Log, Redis Trace, Agent 응답 preview에 남으면 안 되는 민감정보를 마스킹합니다.
 */
@Component
public class SensitiveDataMasker {
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwd", "pwd", "token", "accessToken", "refreshToken", "secret",
            "ssn", "rrn", "residentNumber", "cardNumber", "accountNumber", "accountNo",
            "phone", "mobile", "email", "idempotencyKey");
    private static final Pattern EMAIL = Pattern.compile("([a-zA-Z0-9._%+-]{2})[a-zA-Z0-9._%+-]*(@[a-zA-Z0-9.-]+)");
    private static final Pattern CARD_OR_ACCOUNT = Pattern.compile("\\b(\\d{4})\\d{4,12}(\\d{2,4})\\b");
    private final ObjectMapper json;

    public SensitiveDataMasker(ObjectMapper json) {
        this.json = json;
    }

    /**
     * JsonNode 전체를 재귀적으로 순회하며 민감 key와 민감 패턴을 마스킹합니다.
     */
    public JsonNode mask(JsonNode input) {
        if (input == null || input.isMissingNode() || input.isNull()) {
            return json.createObjectNode();
        }
        if (input.isArray()) {
            ArrayNode masked = json.createArrayNode();
            for (JsonNode item : input) {
                masked.add(mask(item));
            }
            return masked;
        }
        if (input.isObject()) {
            ObjectNode masked = json.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = input.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (isSensitiveKey(key)) {
                    masked.put(key, "***");
                } else {
                    masked.set(key, mask(value));
                }
            }
            return masked;
        }
        if (input.isTextual()) {
            return json.valueToTree(maskText(input.asText()));
        }
        return input;
    }

    private boolean isSensitiveKey(String key) {
        return key != null && SENSITIVE_KEYS.stream().anyMatch(sensitive -> sensitive.equalsIgnoreCase(key));
    }

    private String maskText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String masked = EMAIL.matcher(value).replaceAll("$1***$2");
        return CARD_OR_ACCOUNT.matcher(masked).replaceAll("$1********$2");
    }
}
