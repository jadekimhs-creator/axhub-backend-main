package io.shinhanlife.axhub.biz.mcp.adapter.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

// 2. 응답 DTO
/**
 * @package io.shinhanlife.axhub.biz.mcp.adapter.dto
 * @className JsonRpcResponse
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
@Getter
@Setter
@JsonPropertyOrder({"jsonrpc", "result", "error", "id"})
public class JsonRpcResponse {
    public String jsonrpc = "2.0";
    public Object result;
    public Object error;
    public String id;
}