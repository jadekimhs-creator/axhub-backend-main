package io.shinhanlife.dap.biz.so.atm.dto;

import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @package io.shinhanlife.dap.biz.so.atm.dto
 * @className AthrOutDto
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
@AllArgsConstructor
@Setter
@Builder
@NoArgsConstructor
public class AthrOutDto {
    private List<AthrItemOutDto> tools;
    private List<AthrItemOutDto> knwls;
}