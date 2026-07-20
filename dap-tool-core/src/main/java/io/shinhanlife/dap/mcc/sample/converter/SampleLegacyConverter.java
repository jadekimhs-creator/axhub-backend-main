package io.shinhanlife.dap.mcc.sample.converter;


/**
 * @package io.shinhanlife.dap.mcc.sample.converter
 * @className SampleLegacyConverter
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
import io.shinhanlife.dap.mcc.sample.dto.SampleAiReqDto;
import io.shinhanlife.dap.mcc.sample.dto.SampleLegacyReqDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SampleLegacyConverter {

    // 테스트 환경 등에서 Spring Bean 주입 없이 직접 접근하기 위한 INSTANCE 제공 (IDE 에러 방지용)
    SampleLegacyConverter INSTANCE = Mappers.getMapper(SampleLegacyConverter.class);

    /**
     * AI Agent의 DTO를 MCI 통신용 DTO로 변환합니다.
     * 필드명이 달라도 @Mapping 어노테이션으로 손쉽게 연결할 수 있습니다.
     */
    @Mapping(source = "userId", target = "customerId")
    @Mapping(source = "actionType", target = "interfaceId")
    @Mapping(source = "extraInfo", target = "requestDetails")
    SampleLegacyReqDto toLegacyReq(SampleAiReqDto aiReq);
}
