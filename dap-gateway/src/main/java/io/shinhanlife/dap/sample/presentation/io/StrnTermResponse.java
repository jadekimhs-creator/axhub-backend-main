package io.shinhanlife.dap.sample.presentation.io;

import io.shinhanlife.glow.PageInfo;
import lombok.*;

import java.util.List;

/**
 * @package io.shinhanlife.dap.sample.presentation.io
 * @className StrnTermResponse
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
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StrnTermResponse {

    private List<StrnTerm> strnTerms;
    private PageInfo pageInfo;

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrnTerm{
        private String strnTermHanNm;
        private String strnTermEngcAbrNm;
        private String strnTermEngNm;
        private String strnTermScrnTermNm;
    }
}