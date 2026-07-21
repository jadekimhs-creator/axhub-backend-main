package io.shinhanlife.dap.other.dto;

import io.shinhanlife.dap.mcc.annotation.McpParameter;
import lombok.Data;

/**
 * @package io.shinhanlife.dap.other.dto
 * @className MciSampleRequestDto
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
@Data
public class MciSampleRequestDto {
    @McpParameter(description = "인터페이스 ID (입력 예시: S001)")
    private String interfaceId;

    @McpParameter(description = "메시지 전송 유형 (입력 예시: T001)")
    private String messageType;

    @McpParameter(description = "조회 대상 고객명 (입력 예시: 홍길동)")
    private String customerName;
}
