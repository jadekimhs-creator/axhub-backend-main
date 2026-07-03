package io.shinhanlife.axhub.common.mcp.tool.dto;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerDetailReq {

    @McpParameter(description = "고객명", required = true)
    private String customerName;

    @McpParameter(description = "고객 식별 번호 (CID)")
    private String customerId;
}
