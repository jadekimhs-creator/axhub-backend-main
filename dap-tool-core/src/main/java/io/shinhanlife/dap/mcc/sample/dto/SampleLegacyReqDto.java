package io.shinhanlife.dap.mcc.sample.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @package io.shinhanlife.dap.mcc.sample.dto
 * @className SampleMciReqDto
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
public class SampleLegacyReqDto {
    
    /**
     * MCI 인터페이스 ID (예: MCI0001)
     */
    private String interfaceId;

    /**
     * 고객 식별 번호
     */
    private String customerId;

    /**
     * 요청 세부 파라미터
     */
    private String requestDetails;
}