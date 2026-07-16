package io.shinhanlife.dap.biz.mcp.sample.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

/**
 * @package io.shinhanlife.dap.biz.mcp.sample.dto
 * @className SampleMciResDto
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
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SampleLegacyResDto {
    
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