package io.shinhanlife.dap.biz.mcp.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import io.shinhanlife.dap.biz.mcp.gateway.audit.AuditLogService;
import io.shinhanlife.dap.biz.mcp.gateway.redis.RedisToolTraceService;

@SpringBootTest
@ActiveProfiles("test")
class AxHubGatewayApplicationTests {

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
