package io.shinhanlife.axhub.common.mcp.tool.dto;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillingStatusReq {

    @McpParameter(description = "조회할 청구 접수 번호", required = true)
    private String billingId;
}
