package io.shinhanlife.dap.biz.mcp.gateway.transport;


/**
 * @package io.shinhanlife.dap.biz.mcp.gateway.transport
 * @className ToolInvoker
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
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Tool 서버 호출 transport의 최소 공통 인터페이스입니다.
 */
public interface ToolInvoker {
    JsonNode invoke(Map<String, Object> payload, String targetUrl);
}
