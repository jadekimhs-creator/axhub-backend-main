package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.ContractStatusReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.ContractDetailReq;

@McpTool(
    name = "contract_inquiry_tool", 
    description = "계약 상태 및 상세 정보 조회 툴", 
    group = "biz_channel", 
    routingType = "HTTP"
)
public class ContractInquiryService extends AbstractMcpToolService {

    @McpFunction(name = "status", description = "계약상태 조회", prompt = "김신한 고객의 현재 계약 상태를 조회해줘.", mappingId = "CNTR_001")
    public Object getStatus(ContractStatusReq data) {
        return executeLegacy("HTTP", "CNTR_001", data);
    }

    @McpFunction(name = "detail", description = "계약상세 조회", prompt = "김신한 고객의 계약 상세 내역을 알려줘.", mappingId = "CNTR_002")
    public Object getDetail(ContractDetailReq data) {
        return executeLegacy("HTTP", "CNTR_002", data);
    }
}
