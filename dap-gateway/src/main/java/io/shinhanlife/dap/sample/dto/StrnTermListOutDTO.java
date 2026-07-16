package io.shinhanlife.dap.sample.dto;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


import io.shinhanlife.glow.PageInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @package io.shinhanlife.dap.sample.dto
 * @className StrnTermListOutDTO
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
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StrnTermListOutDTO {
    private List<StrnTerm> strnTerms;
    private PageInfo pageInfo;

    @Builder
    @Getter
    @Setter
    public static class StrnTerm{
        private String strnTermHanNm;
        private String strnTermEngcAbrNm;
        private String strnTermEngNm;
        private String strnTermScrnTermNm;
    }
}