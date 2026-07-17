package io.shinhanlife.dap.biz.mcp.tool.dto;


/**
 * @package io.shinhanlife.dap.biz.mcp.tool.dto
 * @className BalanceReq
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
import io.shinhanlife.dap.biz.mcp.tool.annotation.McpParameter;
import lombok.Data;

@Data
public class BalanceReq {
    @McpParameter(description = "고객의 계좌번호 (- 제외)", required = true)
    private String accountNumber;
}
