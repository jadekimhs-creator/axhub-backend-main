package io.shinhanlife.dap.biz.mcp.tool.service;

import io.shinhanlife.dap.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.dap.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.dap.biz.mcp.tool.dto.ContractStatusReq;
import io.shinhanlife.dap.biz.mcp.tool.dto.ContractDetailReq;

@McpTool(
    routingType = "HTTP",
    categoryKey = "contract"
)
/**
 * @package io.shinhanlife.dap.biz.mcp.tool.service
 * @className ContractInquiryService
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
public class ContractInquiryService extends AbstractMcpToolService {

    @McpFunction(displayName = "contract_status 툴", name = "contract_status", description = "계약상태 조회", prompt = "김신한 고객의 현재 계약 상태를 조회해줘.", mappingId = "CNTR_001")
    public Object getStatus(ContractStatusReq data) {
        return executeLegacy("HTTP", "CNTR_001", data);
    }

    @McpFunction(displayName = "contract_detail 툴", name = "contract_detail", description = "계약상세 조회", prompt = "김신한 고객의 계약 상세 내역을 알려줘.", mappingId = "CNTR_002")
    public Object getDetail(ContractDetailReq data) {
        return executeLegacy("HTTP", "CNTR_002", data);
    }
}