package io.shinhanlife.axhub.biz.so.atm.usecase.impl;

import io.shinhanlife.axhub.biz.so.atm.converter.AccessMgmtConverter;
import io.shinhanlife.axhub.biz.so.atm.domain.service.AccessMgmtService;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleListOutDto;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSaveRequest;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSearchRequest;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSearchResponse;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.RoleListRequest;
import io.shinhanlife.axhub.biz.so.atm.usecase.AccessMgmtUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccessMgmtUseCaseImpl implements AccessMgmtUseCase {

    private final AccessMgmtService accessMgmtService;
    private final AccessMgmtConverter converter;

    @Override
    public List<RoleListOutDto> getRoles(RoleListRequest request) {
        return accessMgmtService.getRoles(converter.toInDto(request));
    }

    @Override
    public AthrSearchResponse getAthr(AthrSearchRequest request) {
        return converter.toResponse(accessMgmtService.getAthr(converter.toInDto(request)));
    }

    @Override
    public void saveAthr(AthrSaveRequest request) {
        accessMgmtService.saveAthr(converter.toInDto(request));
    }
}
