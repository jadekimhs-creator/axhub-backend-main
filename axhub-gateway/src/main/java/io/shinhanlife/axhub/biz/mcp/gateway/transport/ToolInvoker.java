package io.shinhanlife.axhub.biz.mcp.gateway.transport;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Tool 서버 호출 transport의 최소 공통 인터페이스입니다.
 */
public interface ToolInvoker {
    JsonNode invoke(Map<String, Object> payload, String targetUrl);
}
