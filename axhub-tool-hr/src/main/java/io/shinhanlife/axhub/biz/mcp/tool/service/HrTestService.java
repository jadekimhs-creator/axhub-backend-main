package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.HrTestReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.HrTestRes;
import org.springframework.stereotype.Service;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.service
 * @className HrTestService
 * @description AX HUB 시스템 처리 클래스
 * @author system
 * @create 2026.07.13
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.07.13  system    최초생성
 *
 * </pre>
 */
@Service
@McpTool(
    routingType = "HTTP",
    categoryKey = "hr"
)
public class HrTestService extends AbstractMcpToolService {

    @McpFunction(
        displayName = "HrTest 툴",
        name = "hrtest",
        description = "인사 테스트",
        prompt = "인사 테스트 해줘.",
        mappingId = "HR_TEST",
        register = true,
        requiresApproval = false
    )
    public Object execute(HrTestReq req) {
        return executeLegacy("HTTP", "HR_TEST", req);
    }
}
