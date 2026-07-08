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
        description = "각종 양식(엑셀, PDF 등) 샘플 파일의 다운로드 URL을 제공합니다.", 
        prompt = "고객 등록 엑셀 샘플 파일 다운로드 링크 줘.", 
        mappingId = "COM_TMPL_01"
    )
    public Map<String, Object> getTemplateFileUrl(TemplateDownloadReq data) {
        log.info("MCP 툴 호출됨: get_template_file_url, 요청 템플릿 ID: {}", data.getTemplateId());
        
        // 실제 운영 환경에서는 S3 URL이나 내부 파일 다운로드 API URL을 생성합니다.
        // 여기서는 샘플로 가짜(Dummy) URL을 반환합니다.
        String fileName = "sample_" + (data.getTemplateId() != null ? data.getTemplateId().toLowerCase() : "default") + ".xlsx";
        String downloadUrl = "https://axhub-file-server.shinhanlife.io/downloads/" + fileName;
        
        return Map.of(
            "status", "success",
            "fileName", fileName,
            "downloadUrl", downloadUrl,
            "message", "다운로드 링크가 성공적으로 생성되었습니다. AI는 이 링크를 마크다운 형식으로 사용자에게 전달해야 합니다."
        );
    }
}
