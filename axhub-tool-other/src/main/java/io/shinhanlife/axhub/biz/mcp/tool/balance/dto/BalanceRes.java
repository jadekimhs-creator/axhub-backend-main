package io.shinhanlife.axhub.biz.mcp.tool.balance.dto;

import lombok.Data;

@Data
public class BalanceRes {
    private String status;
    private String message;
    private String accountNumber;
    private long balance;
}
