package io.shinhanlife.dap.biz.mcp.sample.dto;


/**
 * @package io.shinhanlife.dap.biz.mcp.sample.dto
 * @className SampleAiReqDto
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
