package io.shinhanlife.axhub.biz.mcp.tool.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.dto
 * @className BondIssueReq
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
public class BondIssueReq {

    @McpParameter(description = "발행할 디지털 증권 금액", required = true)
    private Long amount;

    @McpParameter(description = "발행 대상 계좌 번호", required = true)
    private String targetAccount;
}