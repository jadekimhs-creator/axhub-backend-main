package io.shinhanlife.axhub.biz.mcp.gateway.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RequestValidator {

    // 파이프라인 진입을 위해 반드시 필요한 필수 필드 목록
    private static final List<String> REQUIRED_FIELDS = List.of("traceId", "agentId", "userPrompt");

    public void validate(Map<String, Object> payload) {
        log.info(" [Validator] 파라미터 및 스키마 검증 시작");

        // 1. 페이로드 자체 Null 또는 Empty 체크
        if (payload == null || payload.isEmpty()) {
            log.error(" [Validator] 검증 실패: 페이로드가 비어 있습니다.");
            throw new IllegalArgumentException("요청 페이로드가 존재하지 않습니다.");
        }

        // 2. 필수 파라미터 누락 및 빈 값 검증
        for (String field : REQUIRED_FIELDS) {
            if (!payload.containsKey(field)) {
                log.error(" [Validator] 검증 실패: 필수 키 누락 [{}]", field);
                throw new IllegalArgumentException("필수 파라미터가 누락되었습니다: " + field);
            }

            Object value = payload.get(field);
            if (value == null || value.toString().trim().isEmpty()) {
                log.error(" [Validator] 검증 실패: 필수 키의 값이 비어 있음 [{}]", field);
                throw new IllegalArgumentException("필수 파라미터의 값이 비어있을 수 없습니다: " + field);
            }
        }

        // 3. 비즈니스 로직에 따른 추가 데이터 길이 또는 타입 검증 (예: 프롬프트 길이)
        String traceId = payload.get("traceId").toString();
        String userPrompt = payload.get("userPrompt").toString();

        if (userPrompt.length() > 2000) {
            log.warn(" [Validator] 프롬프트 길이 초과 (Trace ID: {})", traceId);
            throw new IllegalArgumentException("프롬프트 길이는 2000자를 초과할 수 없습니다.");
        }

        log.info(" [Validator] 스키마 검증 완료 (Trace ID: {})", traceId);

        // TODO: 향후 더 복잡한 JSON Schema 파일(schema.json) 기반의 엄격한 검증이 필요해지면,
        // networknt/json-schema-validator 라이브러리를 도입하여 이 부분을 교체할 수 있습니다.
    }
}
