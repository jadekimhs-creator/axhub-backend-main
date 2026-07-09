package io.shinhanlife.axhub.biz.mcp.sample.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class SampleAiReqDto {
    /**
     * AI Agent가 전달하는 자연어 기반의 고객 식별자
     */
    private String userId;

    /**
     * AI Agent가 판별한 액션 유형
     */
    private String actionType;

    /**
     * AI Agent가 추출한 부가 정보
     */
    private String extraInfo;
}
