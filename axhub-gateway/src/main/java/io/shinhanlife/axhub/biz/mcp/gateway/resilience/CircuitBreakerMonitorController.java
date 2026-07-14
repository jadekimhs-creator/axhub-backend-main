package io.shinhanlife.axhub.biz.mcp.gateway.resilience;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MCP Monitor 화면에서 Circuit Breaker 상태를 확인하고 초기화하기 위한 API입니다.
 */
@RestController
@RequestMapping("/internal/circuit-breakers")
public class CircuitBreakerMonitorController {
    private final CircuitBreakerService circuitBreakers;

    public CircuitBreakerMonitorController(CircuitBreakerService circuitBreakers) {
        this.circuitBreakers = circuitBreakers;
    }

    /**
     * Tool별 Circuit Breaker 상태 목록을 반환합니다.
     */
    @GetMapping
    public List<CircuitBreakerSnapshot> snapshots() {
        return circuitBreakers.snapshots();
    }

    /**
     * 모든 Circuit Breaker를 CLOSED 상태로 초기화합니다.
     */
    @PostMapping("/reset")
    public Map<String, Object> reset() {
        circuitBreakers.resetAll();
        return Map.of("reset", true, "items", circuitBreakers.snapshots());
    }
}
