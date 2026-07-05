package io.shinhanlife.axhub.common.session.presentation.io;

import io.shinhanlife.axhub.common.session.dto.SessionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsoResponse {

    private String retCode;
    private SessionDto userInfo;
    private String redirectUrl;

}
