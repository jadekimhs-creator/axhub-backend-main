package io.shinhanlife.axhub.biz.mcp.sample.converter;

import io.shinhanlife.axhub.biz.mcp.sample.dto.SampleAiReqDto;
import io.shinhanlife.axhub.biz.mcp.sample.dto.SampleMciReqDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SampleMciConverter {

    // 테스트 환경 등에서 Spring Bean 주입 없이 직접 접근하기 위한 INSTANCE 제공 (IDE 에러 방지용)
    SampleMciConverter INSTANCE = Mappers.getMapper(SampleMciConverter.class);

    /**
     * AI Agent의 DTO를 MCI 통신용 DTO로 변환합니다.
     * 필드명이 달라도 @Mapping 어노테이션으로 손쉽게 연결할 수 있습니다.
     */
    @Mapping(source = "userId", target = "customerId")
    @Mapping(source = "actionType", target = "interfaceId")
    @Mapping(source = "extraInfo", target = "requestDetails")
    SampleMciReqDto toMciReq(SampleAiReqDto aiReq);
}
