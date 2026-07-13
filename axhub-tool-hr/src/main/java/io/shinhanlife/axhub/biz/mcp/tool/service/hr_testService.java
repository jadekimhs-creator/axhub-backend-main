package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.hr_testReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.hr_testRes;
import org.springframework.stereotype.Service;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.service
 * @className hr_testService
 * @description AX HUB 시스템 처리 클래스
 * @author root
 * @create 2026.07.13
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.07.13  root    최초생성
 *
 * </pre>
 */
@Service
@McpTool(
    routingType = "MCI",
    categoryKey = "common"
)
public class hr_testService extends AbstractMcpToolService {

    @McpFunction(
        displayName = "hr_test 툴",
        name = "hr_test",
        description = "hr 조회 합니다.",
        prompt = "hr 조회 합니다. 해줘.",
        mappingId = "HR_001",
        register = false,
        requiresApproval = false
    )
    public Object execute(hr_testReq req) {
        return executeLegacy("MCI", "HR_001", req);
    }
}
