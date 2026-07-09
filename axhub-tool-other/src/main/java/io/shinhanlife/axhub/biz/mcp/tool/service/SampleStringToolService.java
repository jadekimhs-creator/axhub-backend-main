package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.MciSampleStringRes;
import io.shinhanlife.axhub.biz.mcp.tool.dto.SampleStringReq;
import io.shinhanlife.glow.util.GlowMciParser;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.service
 * @className SampleStringToolService
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
@Service
@McpTool(
    routingType = "MCI_STRING",
    group = "COMMON"
)
public class SampleStringToolService extends AbstractMcpToolService {

    @McpFunction(
        name = "get_sample_string",
        description = "MCI String 버전과 GlowTrgmField 파싱을 테스트하는 샘플 툴입니다.",
        prompt = "MCI 전문(String) 연계 및 고정 길이 파싱 테스트 해줘.",
        mappingId = "TRGM_001"
    )
    public Object execute(SampleStringReq req) {
        // 1. EIMS(Legacy)를 통해 원본 고정 길이 문자열을 받아옵니다.
        Map<String, Object> result = executeLegacy("MCI_STRING", "TRGM_001", req);
        
        if ("ERROR".equals(result.get("status"))) {
            return result;
        }
        
        String rawStringResponse = (String) result.get("legacy_response");
        
        // 2. 받아온 고정 길이 전문(String)을 GlowMciParser를 이용해 DTO로 파싱합니다.
        return GlowMciParser.parse(rawStringResponse, MciSampleStringRes.class);
    }
}
