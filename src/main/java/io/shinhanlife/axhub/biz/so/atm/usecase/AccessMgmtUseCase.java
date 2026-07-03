package io.shinhanlife.axhub.biz.so.atm.usecase;

import io.shinhanlife.axhub.biz.so.atm.dto.RoleListOutDto;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSaveRequest;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSearchRequest;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSearchResponse;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.RoleListRequest;

import java.util.List;

public interface AccessMgmtUseCase {

    List<RoleListOutDto> getRoles(RoleListRequest request);

    AthrSearchResponse getAthr(AthrSearchRequest request);

    void saveAthr(AthrSaveRequest request);
}
