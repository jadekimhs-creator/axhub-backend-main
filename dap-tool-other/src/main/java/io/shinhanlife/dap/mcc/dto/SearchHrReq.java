package io.shinhanlife.dap.mcc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.shinhanlife.dap.mcc.annotation.McpParameter;
import lombok.Data;

/**
 * @package io.shinhanlife.dap.mcc.dto
 * @className SearchHrReq
 * @description AX HUB 시스템 처리 클래스
 * @author user
 * @create 2026.07.22
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.07.22  user    최초생성
 *
 * </pre>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchHrReq {
    @McpParameter(description = "수신자 전화번호", required = true)
    private String phoneNumber;

    @McpParameter(description = "전송할 메시지 내용", required = true)
    private String message;
}
