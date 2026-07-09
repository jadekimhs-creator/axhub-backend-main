package io.shinhanlife.axhub.biz.mcp.tool.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.dto
 * @className BillingProcessReq
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
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingProcessReq {

    @McpParameter(description = "처리할 청구 접수 번호", required = true)
    private String billingId;

    @McpParameter(description = "심사 승인 여부 (예: APPROVE, REJECT)")
    private String approvalStatus;
}