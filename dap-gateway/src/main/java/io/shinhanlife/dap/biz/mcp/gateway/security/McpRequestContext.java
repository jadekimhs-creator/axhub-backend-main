package io.shinhanlife.dap.biz.mcp.gateway.security;


/**
 * @package io.shinhanlife.dap.biz.mcp.gateway.security
 * @className McpRequestContext
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
import java.util.Set;

/**
 * Agent가 MCP 서버로 보낸 헤더 정보를 표준화한 요청 컨텍스트입니다.
 *
 * 이후 권한 검증, 감사 로그, Redis Trace, Tool 서버 호출 시 공통으로 사용됩니다.
 */
public record McpRequestContext(
        String requestId,
        String traceGroupId,
        String clientAddress,
        String agentId,
        String userId,
        Set<String> roles,
        Set<String> allowedTools,
        boolean claimsTrusted,
        boolean writeApproved
) {
}
