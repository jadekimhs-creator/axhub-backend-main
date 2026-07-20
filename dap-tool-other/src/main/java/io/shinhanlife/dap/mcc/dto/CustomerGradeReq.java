package io.shinhanlife.dap.mcc.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.shinhanlife.dap.mcc.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

/**
 * @package io.shinhanlife.dap.mcc.dto
 * @className CustomerGradeReq
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
public class CustomerGradeReq {

    @McpParameter(description = "고객 이름 (예: 김신한)", required = true)
    private String customerName;

    @McpParameter(description = "고객 식별 번호 (CID)")
    private String customerId;
}