package io.shinhanlife.axhub.common.session.converter;

import io.shinhanlife.axhub.common.session.dto.SessionDto;
import io.shinhanlife.axhub.common.session.dto.ZtUsacOutDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class ZtUsacConverter {

    @Mapping(target = "loginDtm", ignore = true)
    @Mapping(target = "isManager", ignore = true)
    public abstract SessionDto toSessionDto(ZtUsacOutDto dto);

}