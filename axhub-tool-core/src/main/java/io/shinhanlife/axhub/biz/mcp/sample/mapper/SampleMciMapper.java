package io.shinhanlife.axhub.biz.mcp.sample.mapper;

import io.shinhanlife.axhub.biz.mcp.sample.dto.SampleAiReqDto;
import io.shinhanlife.axhub.biz.mcp.sample.dto.SampleMciReqDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SampleMciMapper {

    /**
     * AI Agent의 DTO를 MCI 통신용 DTO로 변환합니다.
     * 필드명이 달라도 @Mapping 어노테이션으로 손쉽게 연결할 수 있습니다.
     */
    @Mapping(source = "userId", target = "customerId")
    @Mapping(source = "actionType", target = "interfaceId")
    @Mapping(source = "extraInfo", target = "requestDetails")
    SampleMciReqDto toMciReq(SampleAiReqDto aiReq);
}
