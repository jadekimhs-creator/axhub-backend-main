package io.shinhanlife.dap.sample.dto;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @package io.shinhanlife.dap.sample.dto
 * @className StrnTermListInDTO
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
public class StrnTermListInDTO {
    private String strnTermHanNm;
}