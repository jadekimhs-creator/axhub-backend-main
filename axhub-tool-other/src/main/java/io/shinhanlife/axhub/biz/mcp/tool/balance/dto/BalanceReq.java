package io.shinhanlife.axhub.biz.mcp.tool.balance.dto;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Data;

@Data
public class BalanceReq {
    @McpParameter(description = "고객의 계좌번호 (- 제외)", required = true)
    private String accountNumber;
}
