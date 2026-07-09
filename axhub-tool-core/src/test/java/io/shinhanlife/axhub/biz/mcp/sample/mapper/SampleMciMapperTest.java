package io.shinhanlife.axhub.biz.mcp.sample.mapper;

import io.shinhanlife.axhub.biz.mcp.sample.dto.SampleAiReqDto;
import io.shinhanlife.axhub.biz.mcp.sample.dto.SampleMciReqDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SampleMciMapperTest {

    // MapStruct가 자동 생성한 구현체를 직접 가져와서 테스트합니다. (의존성 주입 불필요)
    private final SampleMciMapper sampleMciMapper = new SampleMciMapperImpl();

    @Test
    @DisplayName("AI 파라미터 DTO가 MCI DTO로 정확히 매핑되는지 테스트")
    public void testAiToMciMapping() {
        // given: AI Agent가 준 깔끔한 형태의 파라미터 생성
        SampleAiReqDto aiDto = SampleAiReqDto.builder()
                .userId("HONG_GILDONG")
                .actionType("SOATM0100R")
                .extraInfo("휴가신청내역 조회")
                .build();

        // when: MapStruct 자동 생성 매퍼를 통해 1줄로 변환
        SampleMciReqDto mciDto = sampleMciMapper.toMciReq(aiDto);

        // then: 콘솔에 결과 출력 및 값 검증
        System.out.println("====== 🚀 MapStruct 변환 테스트 결과 🚀 ======");
        System.out.println("✅ [변환 전] AI DTO : " + aiDto);
        System.out.println("✅ [변환 후] MCI DTO: " + mciDto);
        System.out.println("===============================================");

        assertNotNull(mciDto, "변환된 객체는 null이 아니어야 합니다.");
        assertEquals("HONG_GILDONG", mciDto.getCustomerId(), "userId -> customerId 매핑 성공");
        assertEquals("SOATM0100R", mciDto.getInterfaceId(), "actionType -> interfaceId 매핑 성공");
        assertEquals("휴가신청내역 조회", mciDto.getRequestDetails(), "extraInfo -> requestDetails 매핑 성공");
    }
}
