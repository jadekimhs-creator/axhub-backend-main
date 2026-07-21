package io.shinhanlife.dap.mcc.other.service;

import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import io.shinhanlife.dap.mcc.dto.BondCheckReq;
import io.shinhanlife.dap.mcc.dto.BondIssueReq;
import io.shinhanlife.dap.mcc.service.AbstractMcpToolService;

@McpTool(
    routingType = "EAI",
    categoryKey = "policy"
)
/**
 * @package io.shinhanlife.dap.mcc.service
 * @className BondIssueService
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
public class BondIssueService extends AbstractMcpToolService {

    @McpFunction(displayName = "check 툴", name = "check", register = false, description = "발행 가능 여부 조회 테스트", prompt = "디지털 증권 발행 한도가 충분한지 확인해줘.", mappingId = "BOND_001")
    public Object check(BondCheckReq data) {
        return executeLegacy("EAI", "BOND_001", data);
    }

    @McpFunction(displayName = "issue 툴", name = "issue", register = false, description = "증권 발행 테스트1", prompt = "디지털 증권 발행 프로세스를 실행해.", mappingId = "BOND_002")
    public Object issue(BondIssueReq data) {
        return executeLegacy("EAI", "BOND_002", data);
    }
}