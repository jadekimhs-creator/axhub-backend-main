package io.shinhanlife.axhub.biz.mcp.gateway.security;

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
