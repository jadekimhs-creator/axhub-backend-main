package io.shinhanlife.axhub.biz.so.atm.dto;

import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Setter
@Builder
@NoArgsConstructor
public class AthrOutDto {
    private List<AthrItemOutDto> tools;
    private List<AthrItemOutDto> knwls;
}
