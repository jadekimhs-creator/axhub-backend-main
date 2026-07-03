package io.shinhanlife.axhub.biz.so.atm.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AthrItemOutDto {
    private String  resourceId;
    private String  resourceNm;
    private String  resourceDs;
    private String  typeCode;
    private boolean granted;
}
