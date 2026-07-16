package io.shinhanlife.dap.biz.mcp.adapter.exception;

/**
 * @package io.shinhanlife.dap.biz.mcp.adapter.exception
 * @className MciCommunicationException
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
public class MciCommunicationException extends RuntimeException {
    
    public MciCommunicationException(String message) {
        super(message);
    }
    
    public MciCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
