package io.shinhanlife.dat.mcg.guardrail;


/**
 * @package io.shinhanlife.dat.mcg.guardrail
 * @className GuardrailService
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
import io.shinhanlife.dat.mcg.resilience.FailureType;
import io.shinhanlife.dat.mcg.resilience.ToolExecutionException;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.Iterator;

@Service
/**
 * Tool 호출 인자의 규격을 검증하는 Guardrail 서비스입니다.
 *
 * 필수값 누락, 허용되지 않은 인자 전달 같은 문제를 Tool 서버 호출 전에 차단합니다.
 */
public class GuardrailService {
    /**
     * ToolMetadata에 정의된 required/allowed arguments 기준으로 요청 인자를 검증합니다.
     */
    public void validate(ToolMetadata metadata, ObjectNode arguments) {
        if (metadata.getParametersSchema() == null) {
            // 스키마 정보가 없으면(Fallback 툴 등) 검증을 생략합니다.
            return;
        }

        Iterator<String> fieldNames = arguments.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!metadata.allowedArguments().contains(fieldName)) {
                throw new ToolExecutionException(FailureType.CLIENT_ERROR, "Unknown argument for " + metadata.getName() + ": " + fieldName);
            }
        }
        for (String requiredArgument : metadata.requiredArguments()) {
            JsonNode value = arguments.get(requiredArgument);
            if (isMissing(value)) {
                throw new ToolExecutionException(FailureType.CLIENT_ERROR, "Missing required argument for " + metadata.getName() + ": " + requiredArgument);
            }
        }
    }

    private boolean isMissing(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return true;
        }
        return value.isTextual() && value.asText("").isBlank();
    }
}
