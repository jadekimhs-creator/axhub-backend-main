package io.shinhanlife.axhub.biz.so.atm.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AthrItemOutDto {
    private String  resourceId;
    private String  resourceNm;
    private String  resourceDs;
    private String  typeCode;
    private boolean granted;
}
