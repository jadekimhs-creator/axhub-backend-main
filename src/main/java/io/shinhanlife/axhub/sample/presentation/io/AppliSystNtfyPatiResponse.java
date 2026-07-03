package io.shinhanlife.axhub.sample.presentation.io;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AppliSystNtfyPatiResponse {
    private String successYn;
    private String msg;
}
