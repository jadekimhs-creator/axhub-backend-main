package io.shinhanlife.axhub.sample.dto;


import io.shinhanlife.glow.PageInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
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
