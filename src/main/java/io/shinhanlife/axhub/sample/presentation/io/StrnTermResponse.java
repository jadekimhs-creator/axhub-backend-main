package io.shinhanlife.axhub.sample.presentation.io;

import io.shinhanlife.glow.PageInfo;
import lombok.*;

import java.util.List;

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