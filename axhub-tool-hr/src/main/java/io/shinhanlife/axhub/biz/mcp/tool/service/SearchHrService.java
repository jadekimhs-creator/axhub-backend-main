package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.SearchHrReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.SearchHrRes;
import org.springframework.stereotype.Service;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.service
 * @className SearchHrService
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
public class SearchHrService extends AbstractMcpToolService {

    @McpFunction(
        displayName = "SearchHr 툴",
        name = "SearchHr",
        description = "HR 을 조회 합니다.",
        prompt = "HR 을 조회 합니다. 해줘.",
        mappingId = "SEARCH_HR_001",
        register = false,
        requiresApproval = false
    )
    public Object execute(SearchHrReq req) {
        return executeLegacy("MCI", "SEARCH_HR_001", req);
    }
}
