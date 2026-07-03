package io.shinhanlife.axhub.biz.so.atm.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AthrSaveInDto {
    private String       systId;
    private String       roleNo;
    private List<String> grantedToolIds;
    private List<String> grantedKnwlIds;
}
