package io.shinhanlife.axhub.common.session.domain.service;

import io.shinhanlife.axhub.common.session.dto.ZtUsacInDto;
import io.shinhanlife.axhub.common.session.dto.ZtUsacOutDto;

public interface ZtUsacService {

    /**
     * 사용자 조회 (단건)
     *
     * @param dto 사번
     * @return 인사정보
     */
    ZtUsacOutDto selectZtUsac(ZtUsacInDto dto);
}