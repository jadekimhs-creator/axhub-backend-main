package io.shinhanlife.dap.biz.mcp.tool.dto;

import io.shinhanlife.glow.GlowMciFieldInfo;
import lombok.Data;

/**
 * @package io.shinhanlife.dap.biz.mcp.tool.dto
 * @className MciSampleTargetDto
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 */
@Data
public class MciSampleTargetDto {
    @GlowMciFieldInfo(order = 1, length = 5, description = "항목 코드")
    private String itemCode;

    @GlowMciFieldInfo(order = 2, length = 5, description = "항목 값")
    private String itemValue;
}
