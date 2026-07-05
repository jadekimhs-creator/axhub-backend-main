package io.shinhanlife.axhub.sample.usecase.impl;

import io.shinhanlife.axhub.sample.converter.SampleConverter;
import io.shinhanlife.axhub.sample.domain.service.GlowSampleService;
import io.shinhanlife.axhub.sample.dto.StrnTermListOutDTO;
import io.shinhanlife.axhub.sample.presentation.io.AppliSystNtfyPatiRequest;
import io.shinhanlife.axhub.sample.presentation.io.AppliSystNtfyPatiResponse;
import io.shinhanlife.axhub.sample.presentation.io.StrnTermRequest;
import io.shinhanlife.axhub.sample.presentation.io.StrnTermResponse;
import io.shinhanlife.axhub.sample.usecase.GlowSampleUseCase;
import io.shinhanlife.glow.GlowAppServiceId;
import io.shinhanlife.glow.GlowLogTarget;
import io.shinhanlife.glow.GlowLogger;
import io.shinhanlife.glow.GlowServiceGroupId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@GlowServiceGroupId(value = "GlowSample", description="GlowSampleUseCase")
public class GlowSampleUseCaseImpl implements GlowSampleUseCase {
    @GlowLogTarget(GlowLogTarget.Target.FILE)
    private final GlowLogger log;
    private final GlowSampleService glowSampleService;
    private final SampleConverter converter;

    @GlowAppServiceId(value = "SAMPLE-DB-0001", description = "표준용어조회(DB)")
    public StrnTermResponse getStrnTerms(StrnTermRequest req) {
        StrnTermListOutDTO outDto = glowSampleService.getStrnTerms(converter.convertRequestToDto(req), req.getPageInfo());

        return converter.convertDtoToResponse(outDto);
    }

    @GlowAppServiceId(value = "SAMPLE-DB-0002", description = "어플리케이션시스템알림내역등록")
    public AppliSystNtfyPatiResponse insertAppliSystNtfyPati(AppliSystNtfyPatiRequest req) {
        int rtn = glowSampleService.insertAppliSystNtfyPati(converter.convertAppliRequestToDto(req));

        return AppliSystNtfyPatiResponse.builder()
                .successYn(rtn > 0 ? "Y" : "N")
                .msg(rtn > 0 ? "성공적으로 처리 되었습니다." : "처리중 오류발생!!")
                .build();
    }
}
