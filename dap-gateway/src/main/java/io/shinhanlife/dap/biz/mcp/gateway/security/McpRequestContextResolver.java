package io.shinhanlife.dap.biz.mcp.gateway.security;


/**
 * @package io.shinhanlife.dap.biz.mcp.gateway.security
 * @className McpRequestContextResolver
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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
/**
 * 현재 HTTP 요청에서 Agent/User/권한 관련 헤더를 읽어 McpRequestContext로 변환합니다.
 *
 * MCP Tool 메소드 자체는 HTTP 객체를 직접 받지 않으므로, 이 클래스가 요청 정보를 꺼내주는 역할을 합니다.
 */
public class McpRequestContextResolver {
    /**
     * 현재 요청의 헤더를 읽어 권한/추적용 컨텍스트를 생성합니다.
     */
    public McpRequestContext current() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            String requestId = UUID.randomUUID().toString();
            return new McpRequestContext(requestId, requestId, "internal", "internal", "internal", Set.of(), Set.of(), true, true);
        }
        HttpServletRequest request = attributes.getRequest();
        String requestId = firstNonBlank(request.getHeader("X-Request-Id"), UUID.randomUUID().toString());
        return new McpRequestContext(
                requestId,
                firstNonBlank(request.getHeader("X-MCP-Trace-Group-Id"), requestId),
                request.getRemoteAddr(),
                firstNonBlank(request.getHeader("X-MCP-Agent-Id"), "anonymous"),
                firstNonBlank(request.getHeader("X-MCP-User-Id"), "anonymous"),
                csvHeader(request.getHeader("X-MCP-Roles")),
                csvHeader(request.getHeader("X-MCP-Allowed-Tools")),
                Boolean.parseBoolean(firstNonBlank(request.getHeader("X-MCP-Claims-Trusted"), "false")),
                Boolean.parseBoolean(firstNonBlank(request.getHeader("X-MCP-Write-Approved"), "false")));
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Set<String> csvHeader(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
