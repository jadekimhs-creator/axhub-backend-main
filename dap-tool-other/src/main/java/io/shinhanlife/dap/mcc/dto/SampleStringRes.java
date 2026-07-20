package io.shinhanlife.dap.mcc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.shinhanlife.glow.GlowTrgmField;
import lombok.Data;

/**
 * @package io.shinhanlife.dap.mcc.dto
 * @className SampleStringRes
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SampleStringRes {
    @GlowTrgmField(order = 1, length = 10, description = "이름")
    private String name;

    @GlowTrgmField(order = 2, length = 3, description = "나이")
    private int age;

    @GlowTrgmField(order = 3, length = 8, description = "가입일자(YYYYMMDD)")
    private String joinDate;

    @GlowTrgmField(order = 4, length = 2, description = "상태코드")
    private String statusCode;

    @GlowTrgmField(order = 5, length = 10, description = "타겟")
    private String target;
}
