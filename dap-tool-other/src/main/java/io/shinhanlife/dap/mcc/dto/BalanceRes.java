package io.shinhanlife.dap.mcc.dto;


/**
 * @package io.shinhanlife.dap.mcc.dto
 * @className BalanceRes
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
import lombok.Data;

@Data
public class BalanceRes {
    private String status;
    private String message;
    private String accountNumber;
    private long balance;
}
