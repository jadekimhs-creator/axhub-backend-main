package io.shinhanlife.axhub.common.mcp.adapter.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

// 2. 응답 DTO
@Getter
@Setter
@JsonPropertyOrder({"jsonrpc", "result", "error", "id"})
public class JsonRpcResponse {
    public String jsonrpc = "2.0";
    public Object result;
    public Object error;
    public String id;
}