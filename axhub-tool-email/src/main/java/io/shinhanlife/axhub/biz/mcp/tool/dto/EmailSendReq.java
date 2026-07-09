package io.shinhanlife.axhub.biz.mcp.tool.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.shinhanlife.axhub.biz.mcp.tool.annotation.McpParameter;
import lombok.Getter;
import lombok.Setter;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.dto
 * @className EmailSendReq
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
public class EmailSendReq {

    @McpParameter(description = "수신자 이메일 주소", required = true)
    private String emailAddress;

    @McpParameter(description = "이메일 제목", required = true)
    private String subject;

    @McpParameter(description = "이메일 본문 내용", required = true)
    private String body;
}