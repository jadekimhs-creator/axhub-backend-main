package io.shinhanlife.axhub.biz.mcp.tool.sms.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SmsMciReqDto {
    /**
     * EAI 시스템이 요구하는 수신자 번호 파라미터명
     */
    private String phone;

    /**
     * EAI 시스템이 요구하는 메시지 내용 파라미터명
     */
    private String content;
}
