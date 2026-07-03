package io.shinhanlife.axhub.common.mcp.adapter.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JsonRpcRequest {
    private String jsonrpc;
    private String method;
    private Params params;
    private String id;
}