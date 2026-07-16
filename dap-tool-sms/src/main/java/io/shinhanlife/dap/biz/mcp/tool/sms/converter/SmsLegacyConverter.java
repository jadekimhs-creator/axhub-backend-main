package io.shinhanlife.dap.biz.mcp.tool.sms.converter;

import io.shinhanlife.dap.biz.mcp.tool.dto.SmsSendReq;
import io.shinhanlife.dap.biz.mcp.tool.sms.dto.SmsLegacyReqDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SmsLegacyConverter {

    @Mapping(source = "phoneNumber", target = "phone")
    @Mapping(source = "message", target = "content")
    SmsLegacyReqDto toLegacyReq(SmsSendReq req);
}
