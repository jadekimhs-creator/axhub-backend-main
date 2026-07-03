package io.shinhanlife.axhub.common.mcp.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 클라이언트가 보낸 Trace ID가 있으면 쓰고, 없으면 새로 생성
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            // 간결하게 8자리 UUID만 사용
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }

        // 로깅 컨텍스트에 고유 ID 저장
        MDC.put(MDC_KEY, traceId);

        try {
            // 이 요청이 처리되는 동안 찍히는 모든 log.info, log.error에 traceId가 자동으로 붙습니다.
            filterChain.doFilter(request, response);
        } finally {
            // 메모리 누수 방지를 위해 요청이 끝나면 반드시 비워줍니다.
            MDC.clear();
        }
    }
}
