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
public class RoleListOutDto {
    private String systId;
    private String roleNo;
    private String roleNm;
    private String roleDs;
    private String puseYn;
}
