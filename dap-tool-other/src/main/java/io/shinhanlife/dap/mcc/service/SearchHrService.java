package io.shinhanlife.dap.mcc.service;

import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import io.shinhanlife.dap.mcc.dto.SearchHrReq;
import io.shinhanlife.dap.mcc.dto.SearchHrRes;
import io.shinhanlife.dap.common.integration.mci.component.AxhubMciComponent;
import io.shinhanlife.glow.communication.dto.Transfer;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import org.mapstruct.factory.Mappers;
import io.shinhanlife.dap.mcc.converter.SearchHrLegacyConverter;
import io.shinhanlife.dap.mcc.legacy.SearchHrLegacyReq;

/**
 * @package io.shinhanlife.dap.mcc.service
 * @className SearchHrService
 * @description AX HUB 시스템 처리 클래스
 * @author user
 * @create 2026.07.22
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.07.22  user    최초생성
 *
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@McpTool(
    routingType = "MCI",
    categoryKey = "hr"
)
public class SearchHrService {

    private final AxhubMciComponent mci;
    private final SearchHrLegacyConverter converter = Mappers.getMapper(SearchHrLegacyConverter.class);

    @McpFunction(
        displayName = "SearchHr 툴",
        name = "searchHr",
        description = "hr 조회",
        prompt = "hr 조회 해줘.",
        mappingId = "HR_001",
        register = true,
        requiresApproval = false,
        openWorldHint = true
    )
    public Object execute(SearchHrReq req) {
        log.info("[MCI Tool] {} 요청 수신.", "SearchHr");
        try {
            // MapStruct를 이용한 자동 매핑 (AI DTO -> MCI DTO)
            SearchHrLegacyReq legacyReq = converter.toLegacyReq(req);

            Transfer<Object> resTransfer = mci.callTo(
                    "HR_001",
                    null,
                    legacyReq,
                    Object.class
            );
            return resTransfer.getBody() != null ? resTransfer.getBody() : Map.of("status", "SUCCESS");
        } catch (Exception e) {
            log.error("[MCI Tool] 연동 중 오류 발생: {}", e.getMessage(), e);
            return Map.of("status", "ERROR", "message", e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }
}
