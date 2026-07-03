package io.shinhanlife.axhub.common.mcp.tool.dto;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BondCheckReq {

    @McpParameter(description = "확인하고자 하는 디지털 증권 발행 금액", required = true)
    private Long amount;
}
