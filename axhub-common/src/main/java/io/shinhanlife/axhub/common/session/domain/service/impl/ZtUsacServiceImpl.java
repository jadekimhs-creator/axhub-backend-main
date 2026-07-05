package io.shinhanlife.axhub.common.session.domain.service.impl;

import io.shinhanlife.axhub.common.session.domain.repository.ZtUsacRepository;
import io.shinhanlife.axhub.common.session.domain.service.ZtUsacService;
import io.shinhanlife.axhub.common.session.dto.ZtUsacInDto;
import io.shinhanlife.axhub.common.session.dto.ZtUsacOutDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ZtUsacServiceImpl implements ZtUsacService {

    private final ZtUsacRepository ztUsacRepository;

    /**
     * 사용자 조회 (단건)
     *
     * @param dto 사번
     * @return 인사정보
     */
    @Override
    public ZtUsacOutDto selectZtUsac(ZtUsacInDto dto) {
        ZtUsacOutDto result = ztUsacRepository.selectZtUsac(dto);
        result.initLists();
        return result;
    }
}
