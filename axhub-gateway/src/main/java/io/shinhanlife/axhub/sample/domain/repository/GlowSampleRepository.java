package io.shinhanlife.axhub.sample.domain.repository;

import io.shinhanlife.axhub.sample.domain.model.AppliSystNtfyPatiModel;
import io.shinhanlife.axhub.sample.dto.StrnTermListInDTO;
import io.shinhanlife.axhub.sample.dto.StrnTermListOutDTO;
import io.shinhanlife.glow.GlowIndexPaging;
import io.shinhanlife.glow.GlowMybatisMapper;
import io.shinhanlife.glow.PageInfo;

import java.util.List;

@GlowMybatisMapper
public interface GlowSampleRepository {

    @GlowIndexPaging
    List<StrnTermListOutDTO.StrnTerm> selectStrnTerm(StrnTermListInDTO inDto, PageInfo pageInfo);

    int insertAppliSystNtfyPati(AppliSystNtfyPatiModel appliSystNtfyPatiModel);

}
