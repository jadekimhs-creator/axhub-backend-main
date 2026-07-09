package io.shinhanlife.axhub.common.mcp.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * @package io.shinhanlife.axhub.common.mcp.security
 * @className ApiKeyInterceptor
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
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyInterceptor implements HandlerInterceptor {

    // 1. 다중 테넌트 API Key 목록이 담긴 프로퍼티 객체를 주입받습니다.
    private final SecurityProperties securityProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String apiKey = request.getHeader("X-API-KEY");
        Map<String, String> validApiKeys = securityProperties.getApiKeys();

        // 2. 헤더로 넘어온 API Key가 우리가 발급한 키 목록(Map)에 존재하는지 확인합니다.
        if (apiKey == null || !validApiKeys.containsKey(apiKey)) {
            log.warn(" [보안 차단] 유효하지 않은 API Key 접근 시도 - IP: {}", request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key");
            return false; // 컨트롤러로 넘어가지 않음
        }

        // 3. 유효한 키라면, 해당 키와 맵핑된 Tenant ID(식별자)를 가져옵니다. (ex. mcp-client-1)
        String tenantId = validApiKeys.get(apiKey);

        // 4. 추출한 Tenant ID를 현재 스레드의 로깅 컨텍스트(MDC)에 저장합니다.
        // 이렇게 하면 이 요청이 끝날 때까지 찍히는 모든 로그에 어떤 테넌트가 호출했는지 자동으로 기록됩니다.
        MDC.put("tenantId", tenantId);
        
        // 5. 필요시 컨트롤러 로직에서 사용할 수 있도록 Request 속성에도 담아줍니다.
        request.setAttribute("tenantId", tenantId);
        
        log.debug(" [보안 통과] API Key 인증 성공 - 접속 테넌트: {}", tenantId);

        return true; // 인증 통과! 컨트롤러로 진행
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 6. 메모리 누수를 방지하기 위해 요청 처리가 완전히 끝나면 MDC에서 테넌트 정보를 지워줍니다.
        MDC.remove("tenantId");
    }
}