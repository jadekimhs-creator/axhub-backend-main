package io.shinhanlife.axhub.biz.mcp.tool.sms.converter;

import io.shinhanlife.axhub.biz.mcp.tool.dto.SmsSendReq;
import io.shinhanlife.axhub.biz.mcp.tool.sms.dto.SmsLegacyReqDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SmsMciConverter {

    SmsMciConverter INSTANCE = Mappers.getMapper(SmsMciConverter.class);

    @Mapping(source = "phoneNumber", target = "phone")
    @Mapping(source = "message", target = "content")
    SmsLegacyReqDto toMciReq(SmsSendReq req);
}
