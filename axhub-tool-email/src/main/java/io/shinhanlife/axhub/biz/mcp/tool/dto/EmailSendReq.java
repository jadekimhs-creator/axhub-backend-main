package io.shinhanlife.axhub.biz.mcp.tool.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSendReq {

    @McpParameter(description = "수신자 이메일 주소", required = true)
    private String emailAddress;

    @McpParameter(description = "이메일 제목", required = true)
    private String subject;

    @McpParameter(description = "이메일 본문 내용", required = true)
    private String body;
}
