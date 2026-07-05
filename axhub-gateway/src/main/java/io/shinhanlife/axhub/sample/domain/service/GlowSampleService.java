package io.shinhanlife.axhub.sample.domain.service;

import io.shinhanlife.axhub.sample.dto.AppliSystNtfyRgiInDTO;
import io.shinhanlife.axhub.sample.dto.StrnTermListInDTO;
import io.shinhanlife.axhub.sample.dto.StrnTermListOutDTO;
import io.shinhanlife.glow.PageInfo;

public interface GlowSampleService {
    StrnTermListOutDTO getStrnTerms(StrnTermListInDTO inDto, PageInfo pageInfo);
    int insertAppliSystNtfyPati(AppliSystNtfyRgiInDTO inDto);
}
