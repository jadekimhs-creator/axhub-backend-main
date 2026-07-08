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
public class CustomerGradeReq {

    @McpParameter(description = "고객 이름 (예: 김신한)", required = true)
    private String customerName;

    @McpParameter(description = "고객 식별 번호 (CID)")
    private String customerId;
}
