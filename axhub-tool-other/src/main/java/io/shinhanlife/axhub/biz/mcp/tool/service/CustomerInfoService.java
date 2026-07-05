package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.CustomerGradeReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.CustomerDetailReq;

@McpTool(
    routingType = "TCP"
)
public class CustomerInfoService extends AbstractMcpToolService {

    @McpFunction(name = "grade", description = "고객등급 조회", prompt = "이 고객의 VIP 등급을 조회해줘.", mappingId = "CRM_001")
    public Object getGrade(CustomerGradeReq req) {
        return executeLegacy("TCP", "CRM_001", req);
    }

    @McpFunction(name = "detail", description = "고객상세 정보 조회", prompt = "이 고객의 상세 기본정보(주소, 연락처 등)를 알려줘.", mappingId = "CRM_002")
    public Object getDetail(CustomerDetailReq data) {
        return executeLegacy("TCP", "CRM_002", data);
    }
}
