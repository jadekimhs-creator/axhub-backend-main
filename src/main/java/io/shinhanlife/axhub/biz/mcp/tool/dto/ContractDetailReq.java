package io.shinhanlife.axhub.biz.mcp.tool.dto;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractDetailReq {

    @McpParameter(description = "고객명", required = true)
    private String customerName;

    @McpParameter(description = "조회할 계약 번호", required = true)
    private String contractId;
}
