package io.shinhanlife.axhub.biz.so.atm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AthrOutDto {
    private List<AthrItemOutDto> tools;
    private List<AthrItemOutDto> knwls;
}
