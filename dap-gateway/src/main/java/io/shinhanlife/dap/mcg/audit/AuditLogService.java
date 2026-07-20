package io.shinhanlife.dap.mcg.audit;


/**
 * @package io.shinhanlife.dap.mcg.audit
 * @className AuditLogService
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
import io.shinhanlife.dap.mcg.config.McpGatewayProperties;
import io.shinhanlife.dap.mcg.guardrail.SensitiveDataMasker;
import io.shinhanlife.dap.mcg.security.McpRequestContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
/**
 * MCP Tool 호출 이력을 감사 로그로 남기는 서비스입니다.
 *
 * 금융권 환경에서 누가, 어떤 Agent로, 어떤 Tool을 호출했는지 추적하기 위한 로그를 담당합니다.
 */
public class AuditLogService {
    private static final Logger audit = LoggerFactory.getLogger("MCP_AUDIT");
    private final McpGatewayProperties properties;
    private final SensitiveDataMasker masker;

    public AuditLogService(McpGatewayProperties properties, SensitiveDataMasker masker) {
        this.properties = properties;
        this.masker = masker;
    }

    /**
     * Tool 실행 시작 시점에 요청자와 마스킹된 인자를 기록합니다.
     */
    public void toolStarted(McpRequestContext context, String toolName, JsonNode arguments) {
        if (!properties.auditEnabled()) {
            return;
        }
        audit.info("event=tool_started requestId={} agentId={} userId={} clientAddress={} tool={} arguments={}",
                context.requestId(), context.agentId(), context.userId(), context.clientAddress(), toolName, masker.mask(arguments));
    }

    /**
     * Tool 실행 종료 시점에 성공 여부, 처리 시간, 오류 유형을 기록합니다.
     */
    public void toolFinished(McpRequestContext context, String toolName, long elapsedMillis, boolean success, String errorCode) {
        if (!properties.auditEnabled()) {
            return;
        }
        audit.info("event=tool_finished requestId={} agentId={} userId={} clientAddress={} tool={} elapsedMillis={} success={} errorCode={}",
                context.requestId(), context.agentId(), context.userId(), context.clientAddress(), toolName, elapsedMillis, success, errorCode);
    }
}
