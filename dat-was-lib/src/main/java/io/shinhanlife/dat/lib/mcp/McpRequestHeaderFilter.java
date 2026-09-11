package io.shinhanlife.dat.lib.mcp;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.shinhanlife.dat.lib.session.mci.AxhubSessionService;
import io.shinhanlife.dat.lib.session.dto.SessionDto;
import org.springframework.beans.factory.annotation.Autowired;

/** Captures optional correlation and employee headers for an MCP HTTP call and creates mock session. */
public class McpRequestHeaderFilter extends OncePerRequestFilter {

    private final AxhubSessionService sessionService;

    public McpRequestHeaderFilter(@Autowired(required = false) AxhubSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().endsWith("/mcp");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String empNo = request.getHeader("employee-no");
        
        McpRequestHeaderContext.set(new McpRequestHeaders(
                request.getHeader("x-request-id"),
                request.getHeader("guid"),
                request.getHeader("mcp-session-id"),
                empNo,
                request.getHeader("virtual-employee-no")));
                
        // Fetch session info via MCI using employee-no (Only if sessionService is available, e.g. in Tool Pods)
        if (sessionService != null && empNo != null && !empNo.isEmpty()) {
            SessionDto sessionDto = sessionService.fetchUserSession(empNo);
            if (sessionDto != null) {
                // Set to request attribute so SessionUtil can find it
                request.setAttribute("userInfo", sessionDto);
            }
        }
                
        try {
            filterChain.doFilter(request, response);
        } finally {
            McpRequestHeaderContext.clear();
        }
    }
}
