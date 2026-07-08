package io.shinhanlife.axhub.biz.mcp.sample.dto;

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
public class SampleMciReqDto {
    
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
