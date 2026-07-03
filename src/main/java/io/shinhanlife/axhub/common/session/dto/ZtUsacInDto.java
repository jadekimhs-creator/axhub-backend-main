package io.shinhanlife.axhub.common.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ZtUsacInDto {

    public ZtUsacInDto() {
        setPuseYn("Y");
    }

    /* 인사번호 */
    private String prafNo;

    /* 사용여부 */
    private String puseYn;

}
