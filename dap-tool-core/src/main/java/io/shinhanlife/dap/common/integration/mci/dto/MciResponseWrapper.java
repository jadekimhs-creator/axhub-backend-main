package io.shinhanlife.dap.common.integration.mci.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @package io.shinhanlife.dap.common.integration.mci.dto
 * @className MciResponseWrapper
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MciResponseWrapper<T> {
    private ShinhanCommonHeaderDto tgrmCmnnhddValu;
    
    @JsonUnwrapped
    private T body;
}
