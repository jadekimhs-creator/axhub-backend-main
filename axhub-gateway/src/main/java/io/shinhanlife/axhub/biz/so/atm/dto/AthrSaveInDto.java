package io.shinhanlife.axhub.biz.so.atm.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AthrSaveInDto {
    private String       systId;
    private String       roleNo;
    private List<String> grantedToolIds;
    private List<String> grantedKnwlIds;
}
