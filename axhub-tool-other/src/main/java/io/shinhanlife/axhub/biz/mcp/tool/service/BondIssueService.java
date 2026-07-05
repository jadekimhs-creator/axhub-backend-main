package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.BondCheckReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.BondIssueReq;

@McpTool(
    routingType = "EAI"
)
public class BondIssueService extends AbstractMcpToolService {

    @McpFunction(name = "check", description = "발행 가능 여부 조회", prompt = "디지털 증권 발행 한도가 충분한지 확인해줘.", mappingId = "BOND_001")
    public Object check(BondCheckReq data) {
        return executeLegacy("EAI", "BOND_001", data);
    }

    @McpFunction(name = "issue", description = "증권 발행", prompt = "디지털 증권 발행 프로세스를 실행해.", mappingId = "BOND_002")
    public Object issue(BondIssueReq data) {
        return executeLegacy("EAI", "BOND_002", data);
    }
}
