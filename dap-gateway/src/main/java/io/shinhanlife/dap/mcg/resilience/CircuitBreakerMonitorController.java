package io.shinhanlife.dap.mcg.resilience;


/**
 * @package io.shinhanlife.dap.mcg.resilience
 * @className CircuitBreakerMonitorController
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
