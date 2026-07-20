package io.shinhanlife.dap.mcc.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @package io.shinhanlife.dap.mcc.dto
 * @className TemplateDownloadReq
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