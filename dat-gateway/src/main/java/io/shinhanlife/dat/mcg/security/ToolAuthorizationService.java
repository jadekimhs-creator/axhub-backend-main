package io.shinhanlife.dat.mcg.security;


/**
 * @package io.shinhanlife.dat.mcg.security
 * @className ToolAuthorizationService
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
import io.shinhanlife.dat.mcg.config.McpGatewayProperties;
import io.shinhanlife.dat.mcg.resilience.FailureType;
import io.shinhanlife.dat.mcg.resilience.ToolExecutionException;
import io.shinhanlife.dat.lib.dto.OperationType;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
/**
 * Agent가 특정 Tool을 호출할 수 있는지 판단하는 권한 검증 서비스입니다.
 *
 * 서버 전체 허용 Tool, Agent가 보낸 allowed-tools 헤더, WRITE 승인 여부를 함께 확인합니다.
 */
public class ToolAuthorizationService {
    private final McpGatewayProperties properties;

    public ToolAuthorizationService(McpGatewayProperties properties) {
        this.properties = properties;
    }

    /**
     * Tool 실행 직전에 호출 가능 여부를 검사하고, 거부 시 ToolExecutionException을 발생시킵니다.
     */
    public void authorize(McpRequestContext context, ToolMetadata metadata) {
        if (properties.agentClaimsRequired() && context.allowedTools().isEmpty()) {
            throw new ToolExecutionException(FailureType.AUTHORIZATION_ERROR, "Missing agent authorization claims");
        }
        if (properties.trustedClaimsRequired() && !context.claimsTrusted()) {
            throw new ToolExecutionException(FailureType.AUTHORIZATION_ERROR, "Agent authorization claims are not trusted");
        }
        Set<String> serverAllowedTools = properties.allowedToolSet();
        if (!isAllowed(serverAllowedTools, metadata.getName())) {
            throw new ToolExecutionException(FailureType.AUTHORIZATION_ERROR, "Tool is not allowed by server policy: " + metadata.getName());
        }
        if (!isAllowed(context.allowedTools(), metadata.getName())) {
            throw new ToolExecutionException(FailureType.AUTHORIZATION_ERROR, "Tool is not allowed by agent claims: " + metadata.getName());
        }
        if (properties.writeApprovalRequired()
                && (metadata.getOperationType() == OperationType.WRITE || (metadata.getRequiresApproval() != null && metadata.getRequiresApproval()))
                && !context.writeApproved()) {
            throw new ToolExecutionException(FailureType.AUTHORIZATION_ERROR, "Write tool requires approval: " + metadata.getName());
        }
    }

    /**
     * 빈 목록은 제한 없음, "*"는 모든 Tool 허용을 의미합니다.
     */
    private boolean isAllowed(Set<String> allowedTools, String toolName) {
        return allowedTools.isEmpty() || allowedTools.contains("*") || allowedTools.contains(toolName);
    }
}
