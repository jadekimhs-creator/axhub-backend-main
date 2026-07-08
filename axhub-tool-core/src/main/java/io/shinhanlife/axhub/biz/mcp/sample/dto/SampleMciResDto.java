package io.shinhanlife.axhub.biz.mcp.sample.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SampleMciResDto {
    
    /**
     * 응답 코드 (예: "0000"이면 성공)
     */
    private String resCode;

    /**
     * 응답 메시지
     */
    private String resMsg;

    /**
     * 상세 데이터
     */
    private Map<String, Object> data;
}
