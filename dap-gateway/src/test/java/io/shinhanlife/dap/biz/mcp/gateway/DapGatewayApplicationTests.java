package io.shinhanlife.dap.mcg;


/**
 * @package io.shinhanlife.dap.mcg
 * @className DapGatewayApplicationTests
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
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import io.shinhanlife.dap.mcg.audit.AuditLogService;
import io.shinhanlife.dap.mcg.redis.RedisToolTraceService;

@SpringBootTest
@ActiveProfiles("test")
class DapGatewayApplicationTests {

    // Mock components that might require external dependencies (like Redis/DB) to pass the context load
    @MockitoBean
    private RedisToolTraceService redisToolTraceService;

    @MockitoBean
    private AuditLogService auditLogService;

    @Test
    void contextLoads() {
        // This test ensures that all Spring beans, @ConfigurationProperties, and dependencies are correctly wired.
        // It validates that the ported classes (ExecuteService, AgentResponseBudgetService, LargeToolResponseService, etc.)
        // have no @Autowired or initialization errors.
    }
}
