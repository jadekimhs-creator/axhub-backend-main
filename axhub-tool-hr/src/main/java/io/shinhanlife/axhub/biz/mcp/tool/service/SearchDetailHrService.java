package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.SearchDetailHrReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.SearchDetailHrRes;
import org.springframework.stereotype.Service;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.service
 * @className SearchDetailHrService
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
    categoryKey = "hr"
)
public class SearchDetailHrService extends AbstractMcpToolService {

    @McpFunction(
        displayName = "SearchDetailHr 툴",
        name = "searchDetailhr",
        description = "hr 상세 조회 합니다.",
        prompt = "hr 상세 조회 합니다. 해줘.",
        mappingId = "SEARCH_HR_002",
        register = false,
        requiresApproval = false
    )
    public Object execute(SearchDetailHrReq req) {
        return executeLegacy("MCI", "SEARCH_HR_002", req);
    }
}
