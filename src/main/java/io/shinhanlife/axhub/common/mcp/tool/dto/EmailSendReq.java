package io.shinhanlife.axhub.common.mcp.tool.dto;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailSendReq {

    @McpParameter(description = "수신자 이메일 주소", required = true)
    private String emailAddress;

    @McpParameter(description = "이메일 제목", required = true)
    private String subject;

    @McpParameter(description = "이메일 본문 내용", required = true)
    private String body;
}
