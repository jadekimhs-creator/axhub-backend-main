package io.shinhanlife.axhub.common.session.domain.repository;

import io.shinhanlife.glow.GlowMybatisMapper;
import io.shinhanlife.axhub.common.session.dto.ZtUsacInDto;
import io.shinhanlife.axhub.common.session.dto.ZtUsacOutDto;


@GlowMybatisMapper
public interface ZtUsacRepository {

    /**
     * 사용자 조회 (단건)
     *
     * @param dto 사번
     * @return 인사정보
     */
    ZtUsacOutDto selectZtUsac(ZtUsacInDto dto);

}