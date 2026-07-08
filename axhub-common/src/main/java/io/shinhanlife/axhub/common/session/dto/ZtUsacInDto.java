package io.shinhanlife.axhub.common.session.dto;

import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ZtUsacInDto {
    /* 인사번호 */
    private String prafNo;

    /* 사용여부 */
    @Builder.Default
    private String puseYn = "Y";

}
