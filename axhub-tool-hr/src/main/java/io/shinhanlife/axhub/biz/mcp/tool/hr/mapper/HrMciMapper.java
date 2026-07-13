package io.shinhanlife.axhub.biz.mcp.tool.hr.mapper;

import io.shinhanlife.axhub.biz.mcp.tool.dto.SmsSendReq;
import io.shinhanlife.axhub.biz.mcp.tool.sms.dto.SmsMciReqDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface HrMciMapper {

    HrMciMapper INSTANCE = Mappers.getMapper(HrMciMapper.class);

    @Mapping(source = "phoneNumber", target = "phone")
    @Mapping(source = "message", target = "content")
    SmsMciReqDto toMciReq(SmsSendReq req);
}
