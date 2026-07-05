package io.shinhanlife.axhub.biz.so.atm.presentation.io;

import io.shinhanlife.axhub.biz.so.atm.dto.AthrItemOutDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AthrSearchResponse {
    private List<AthrItemOutDto> tools;
    private List<AthrItemOutDto> knwls;
}
