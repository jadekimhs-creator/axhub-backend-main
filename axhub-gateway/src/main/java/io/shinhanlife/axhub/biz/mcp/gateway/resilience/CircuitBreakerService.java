package io.shinhanlife.axhub.biz.mcp.gateway.resilience;

import io.shinhanlife.axhub.biz.mcp.gateway.config.McpGatewayProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
/**
 * Tool 이름별 Circuit Breaker 인스턴스를 관리하는 서비스입니다.
 *
 * registry에 등록된 Tool별로 장애 상태를 독립적으로 관리합니다.
 */
public class CircuitBreakerService {
    private final McpGatewayProperties properties;
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    public CircuitBreakerService(McpGatewayProperties properties) {
        this.properties = properties;
    }

    /**
     * 이름에 해당하는 Circuit Breaker를 반환하고, 없으면 새로 생성합니다.
     */
    public CircuitBreaker breaker(String name) {
        return breaker(name, 0, 0);
    }

    /**
     * Tool별 정책이 있으면 해당 threshold/open 시간을 우선 사용해 Circuit Breaker를 생성합니다.
     */
    public CircuitBreaker breaker(String name, int failureThreshold, long openMillis) {
        int effectiveThreshold = failureThreshold > 0 ? failureThreshold : properties.circuitBreakerFailureThreshold();
        long effectiveOpenMillis = openMillis > 0 ? openMillis : properties.circuitBreakerOpenMillis();
        return breakers.computeIfAbsent(name, key -> new CircuitBreaker(
                key,
                effectiveThreshold,
                effectiveOpenMillis,
                Clock.systemUTC()));
    }

    /**
     * Tool registry가 갱신될 때 Circuit Breaker 정책도 새 값으로 갱신합니다.
     */
    public void refresh(String name, int failureThreshold, long openMillis) {
        int effectiveThreshold = failureThreshold > 0 ? failureThreshold : properties.circuitBreakerFailureThreshold();
        long effectiveOpenMillis = openMillis > 0 ? openMillis : properties.circuitBreakerOpenMillis();
        breakers.put(name, new CircuitBreaker(name, effectiveThreshold, effectiveOpenMillis, Clock.systemUTC()));
    }

    /**
     * MCP Monitor 화면에 표시할 전체 Circuit Breaker 상태를 반환합니다.
     */
    public List<CircuitBreakerSnapshot> snapshots() {
        return breakers.values().stream()
                .map(CircuitBreaker::snapshot)
                .sorted(Comparator.comparing(CircuitBreakerSnapshot::name))
                .toList();
    }

    /**
     * 테스트 후 모든 Circuit Breaker 상태를 CLOSED로 초기화합니다.
     */
    public void resetAll() {
        breakers.values().forEach(CircuitBreaker::reset);
    }
}
