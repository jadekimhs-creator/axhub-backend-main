package io.shinhanlife.dat.lib.integration.mci.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * @package io.shinhanlife.dat.lib.integration.mci.dto
 * @className ShinhanTelegramWrapper
 * @description AX HUB 시스템 처리 클래스 - MCI 전문 전체 래퍼 (공통헤더부 + 메시지부 + 데이터부)
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 * 
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShinhanTelegramWrapper<T> {

    // 1. 공통 헤더부
    private ShinhanCommonHeaderDto tgrmCmnnhddValu;

    // 2. 메시지부
    private ShinhanMessageDto tgrmMsdvValu;

    // 3. 데이터부 (비즈니스마다 다름, JsonUnwrapped로 평탄화하거나 객체 자체로 유지 가능. 여기서는 객체 유지)
    private T tgrmDtdvValu;
}
