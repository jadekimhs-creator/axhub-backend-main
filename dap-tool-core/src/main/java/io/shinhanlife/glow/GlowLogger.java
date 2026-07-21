package io.shinhanlife.glow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @package io.shinhanlife.glow
 * @className GlowLogger
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
@Component
@Scope("prototype")
public class GlowLogger {

    private Logger log = LoggerFactory.getLogger(GlowLogger.class);

    public void debug(String message, Object... args) { log.debug(message, args); }
    public void info(String message, Object... args) { log.info(message, args); }
    public void warn(String message, Object... args) { log.warn(message, args); }
    public void error(String message, Object... args) { log.error(message, args); }
    public void error(String message, Throwable t) { log.error(message, t); }
}