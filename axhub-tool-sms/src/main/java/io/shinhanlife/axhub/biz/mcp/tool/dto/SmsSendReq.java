package io.shinhanlife.axhub.biz.mcp.tool.dto;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsSendReq {

    @McpParameter(description = "수신자 전화번호", required = true)
    private String phoneNumber;

    @McpParameter(description = "전송할 메시지 내용", required = true)
    private String message;
}
