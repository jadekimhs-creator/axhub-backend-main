package io.shinhanlife.axhub.biz.mcp.tool.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TemplateDownloadReq {
    
    /**
     * 다운로드할 템플릿의 종류 ID (예: CUSTOMER_EXCEL, PRODUCT_PDF 등)
     */
    private String templateId;
}
