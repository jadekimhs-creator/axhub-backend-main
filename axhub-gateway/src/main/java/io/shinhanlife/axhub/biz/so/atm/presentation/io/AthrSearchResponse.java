package io.shinhanlife.axhub.biz.so.atm.presentation.io;

import io.shinhanlife.axhub.biz.so.atm.dto.AthrItemOutDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @package io.shinhanlife.axhub.biz.so.atm.presentation.io
 * @className AthrSearchResponse
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
public class AthrSearchResponse {
    private List<AthrItemOutDto> tools;
    private List<AthrItemOutDto> knwls;
}