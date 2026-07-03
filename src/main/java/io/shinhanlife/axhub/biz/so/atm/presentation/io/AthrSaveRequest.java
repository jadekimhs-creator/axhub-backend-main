package io.shinhanlife.axhub.biz.so.atm.presentation.io;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AthrSaveRequest {
    private String       systId;
    private String       roleNo;
    private List<String> grantedToolIds;
    private List<String> grantedKnwlIds;
}
