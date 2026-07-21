package io.shinhanlife.dap.mcc.dto;

import lombok.Data;

/**
 * @package io.shinhanlife.dap.mcc.dto
 * @className Onnba3011ReqDto
 * @description 보종By가입설계한도계산조회 MCI 요청 DTO
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
@Data
public class Onnba3011ReqDto {
    // 실제 가입설계한도계산 조회에 필요한 필드
    private String customerId; 
}
