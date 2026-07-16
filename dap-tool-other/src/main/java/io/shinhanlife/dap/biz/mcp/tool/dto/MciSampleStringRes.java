package io.shinhanlife.dap.biz.mcp.tool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.shinhanlife.glow.GlowMciFieldInfo;
import lombok.Data;
import java.util.List;

/**
 * @package io.shinhanlife.dap.biz.mcp.tool.dto
 * @className MciSampleStringRes
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MciSampleStringRes {
    @GlowMciFieldInfo(order = 1, length = 10, description = "이름")
    private String name;

    @GlowMciFieldInfo(order = 2, length = 3, description = "나이")
    private int age;

    @GlowMciFieldInfo(order = 3, length = 8, description = "가입일자(YYYYMMDD)")
    private String joinDate;

    @GlowMciFieldInfo(order = 4, length = 2, description = "상태코드")
    private String statusCode;

    @GlowMciFieldInfo(order = 5, length = 30, description = "타겟 리스트", target = MciSampleTargetDto.class)
    private List<MciSampleTargetDto> targetList;
}
