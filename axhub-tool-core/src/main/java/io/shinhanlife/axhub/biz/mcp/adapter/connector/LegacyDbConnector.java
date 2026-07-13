package io.shinhanlife.axhub.biz.mcp.adapter.connector;

import io.shinhanlife.axhub.biz.mcp.adapter.support.ResultStandardizer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @package io.shinhanlife.axhub.biz.mcp.adapter.connector
 * @className LegacyDbConnector
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
@Service
@RequiredArgsConstructor
public class LegacyDbConnector {

    private final NamedParameterJdbcTemplate jdbcTemplate; // 동적 파라미터 바인딩을 위한 템플릿
    private final ResultStandardizer resultStandardizer;

    @RateLimiter(name = "legacyDb", fallbackMethod = "fallbackForDb")
    @CircuitBreaker(name = "legacyDb", fallbackMethod = "fallbackForDb")
    public List<Map<String, Object>> executeDynamicQuery(String queryId, String sql, Map<String, Object> params) {
        log.info(" Legacy DB 조회 시작 [QueryID: {}]", queryId);

        // 1. SQL 쿼리 실행 (오라클 등)
        List<Map<String, Object>> rawResults = jdbcTemplate.queryForList(sql, params);

        // 2. 스키마 매핑 및 결과 정형화 (대문자 -> 카멜케이스 변환)
        List<Map<String, Object>> standardResults = rawResults.stream()
                .map(resultStandardizer::standardize)
                .collect(Collectors.toList());

        log.info(" Legacy DB 조회 완료 ({}건 반환)", standardResults.size());
        return standardResults;
    }

    public List<Map<String, Object>> fallbackForDb(String queryId, String sql, Map<String, Object> params, Throwable t) {
        log.error(" [Legacy DB 장애/지연] 쿼리 실행 실패 [{}]: {}", queryId, t.getMessage());
        throw new RuntimeException("레거시 DB 연동 중 오류가 발생했습니다. (QueryID: " + queryId + ")");
    }
}