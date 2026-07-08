package io.shinhanlife.axhub.biz.mcp.tool.service;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpFunction;
import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpTool;
import io.shinhanlife.axhub.biz.mcp.tool.dto.TemplateDownloadReq;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
@McpTool(
    routingType = "HTTP",
    group = "COMMON"
)
public class TemplateUtilityService extends AbstractMcpToolService {

    @McpFunction(
        name = "get_template_file_url",
        description = "특정 템플릿의 양식 파일(엑셀, 워드 등)을 다운로드 받을 수 있는 시스템 URL을 반환합니다. AI는 이 URL을 사용자에게 마크다운 링크 형태로 제공해야 합니다.",
        prompt = "요청하신 템플릿 양식 파일 다운로드 URL은 다음과 같습니다. 클릭하여 다운로드하세요:",
        visible = false
    )
    public Map<String, Object> getTemplateFileUrl(TemplateDownloadReq data) {
        try {
            String templateId = (data != null && data.getTemplateId() != null) ? data.getTemplateId().toLowerCase() : "default";
            log.info("MCP 툴 호출됨: get_template_file_url, 요청 템플릿 ID: {}", templateId);
            
            String fileName = "sample_" + templateId + ".xlsx";
            String downloadUrl = "https://axhub-file-server.shinhanlife.io/downloads/" + fileName;
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("status", "success");
            result.put("fileName", fileName);
            result.put("downloadUrl", downloadUrl);
            result.put("message", "다운로드 링크가 성공적으로 생성되었습니다. AI는 이 링크를 마크다운 형식으로 사용자에게 전달해야 합니다.");
            
            return result;
        } catch (Exception e) {
            log.error("getTemplateFileUrl 내부 예외 발생", e);
            throw new RuntimeException("템플릿 URL 생성 실패", e);
        }
    }
}
