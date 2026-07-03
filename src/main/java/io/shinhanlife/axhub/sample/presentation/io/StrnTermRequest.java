package io.shinhanlife.axhub.sample.presentation.io;

import io.shinhanlife.glow.PageInfo;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StrnTermRequest {
    private PageInfo pageInfo;

    private String strnTermHanNm;
}
