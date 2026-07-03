package io.shinhanlife.axhub.biz.so.atm.converter;

import io.shinhanlife.axhub.biz.so.atm.dto.AthrOutDto;
import io.shinhanlife.axhub.biz.so.atm.dto.AthrSaveInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.AthrSearchInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleListInDto;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSaveRequest;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSearchRequest;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSearchResponse;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.RoleListRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class AccessMgmtConverter {

    public abstract RoleListInDto toInDto(RoleListRequest request);

    public abstract AthrSearchInDto toInDto(AthrSearchRequest request);

    public abstract AthrSaveInDto toInDto(AthrSaveRequest request);

    public abstract AthrSearchResponse toResponse(AthrOutDto outDto);
}
