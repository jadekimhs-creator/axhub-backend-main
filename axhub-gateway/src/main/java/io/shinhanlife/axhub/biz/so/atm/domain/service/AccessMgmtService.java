package io.shinhanlife.axhub.biz.so.atm.domain.service;

import io.shinhanlife.axhub.biz.so.atm.dto.AthrOutDto;
import io.shinhanlife.axhub.biz.so.atm.dto.AthrSaveInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.AthrSearchInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleListInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleListOutDto;

import java.util.List;

public interface AccessMgmtService {

    List<RoleListOutDto> getRoles(RoleListInDto inDto);

    AthrOutDto getAthr(AthrSearchInDto inDto);

    void saveAthr(AthrSaveInDto inDto);
}
