package io.shinhanlife.axhub.common.mcp.tool.dto;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillingProcessReq {

    @McpParameter(description = "처리할 청구 접수 번호", required = true)
    private String billingId;

    @McpParameter(description = "심사 승인 여부 (예: APPROVE, REJECT)")
    private String approvalStatus;
}
