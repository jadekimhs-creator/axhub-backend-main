package io.shinhanlife.axhub.biz.mcp.tool.dto;

import io.shinhanlife.glow.GlowTrgmField;
import lombok.Data;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.dto
 * @className SampleTargetDto
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
public class SampleTargetDto {
    @GlowTrgmField(order = 1, length = 5, description = "항목 코드")
    private String itemCode;

    @GlowTrgmField(order = 2, length = 5, description = "항목 값")
    private String itemValue;
}
