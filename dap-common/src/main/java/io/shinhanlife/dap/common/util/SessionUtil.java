package io.shinhanlife.dap.common.util;

import io.shinhanlife.dap.common.session.dto.SessionDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @package io.shinhanlife.dap.common.util
 * @className SessionUtil
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
public class SessionUtil {

    private static final String SESSION_KEY = "userInfo";

    private SessionUtil() {}

    public static SessionDto getSession() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return null;
        HttpSession session = attributes.getRequest().getSession(false);
        if (session == null) return null;
        return (SessionDto) session.getAttribute(SESSION_KEY);
    }

    public static String getPrafNo() {
        SessionDto session = getSession();
        return session != null ? session.getPrafNo() : null;
    }

    public static String getOgnzNo() {
        SessionDto session = getSession();
        return session != null ? session.getOgnzNo() : null;
    }
}