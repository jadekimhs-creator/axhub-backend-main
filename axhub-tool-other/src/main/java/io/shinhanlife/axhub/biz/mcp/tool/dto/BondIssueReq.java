package io.shinhanlife.axhub.biz.mcp.tool.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BondIssueReq {

    @McpParameter(description = "발행할 디지털 증권 금액", required = true)
    private Long amount;

    @McpParameter(description = "발행 대상 계좌 번호", required = true)
    private String targetAccount;
}
