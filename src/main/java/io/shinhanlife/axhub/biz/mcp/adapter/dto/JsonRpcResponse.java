package io.shinhanlife.axhub.biz.mcp.adapter.dto;

import lombok.Getter;
import lombok.Setter;

// 2. 응답 DTO
@Getter
@Setter
public class JsonRpcResponse {
    public String jsonrpc = "2.0";
    public Object result;
    public Object error;
    public String id;
}