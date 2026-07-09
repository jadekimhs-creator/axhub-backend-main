package io.shinhanlife.axhub.sample.domain.service.impl;

import io.shinhanlife.axhub.sample.converter.SampleConverter;
import io.shinhanlife.axhub.sample.domain.model.AppliSystNtfyPatiModel;
import io.shinhanlife.axhub.sample.domain.repository.GlowSampleRepository;
import io.shinhanlife.axhub.sample.domain.service.GlowSampleService;
import io.shinhanlife.axhub.sample.dto.AppliSystNtfyRgiInDTO;
import io.shinhanlife.axhub.sample.dto.StrnTermListInDTO;
import io.shinhanlife.axhub.sample.dto.StrnTermListOutDTO;
import io.shinhanlife.glow.GlowLogger;
import io.shinhanlife.glow.GlowLogTarget;
import io.shinhanlife.glow.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * @package io.shinhanlife.axhub.sample.domain.service.impl
 * @className GlowSampleServiceImpl
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
@Service
@RequiredArgsConstructor
public class GlowSampleServiceImpl implements GlowSampleService {

    @GlowLogTarget(GlowLogTarget.Target.FILE)
    private final GlowLogger log;
    private final GlowSampleRepository glowSampleRepository;
    private final SampleConverter sampleConverter;

    @Override
    public StrnTermListOutDTO getStrnTerms(StrnTermListInDTO inDto, PageInfo pageInfo) {
        List<StrnTermListOutDTO.StrnTerm> out = glowSampleRepository.selectStrnTerm(inDto, pageInfo);
        return StrnTermListOutDTO.builder()
                .strnTerms(out)
                .pageInfo(pageInfo)
                .build();
    }

    @Override
    public int insertAppliSystNtfyPati(AppliSystNtfyRgiInDTO inDto) {
        AppliSystNtfyPatiModel appliSystNtfyPatiModel = sampleConverter.convertDtoToModel(inDto);
        return glowSampleRepository.insertAppliSystNtfyPati(appliSystNtfyPatiModel);
    }

}