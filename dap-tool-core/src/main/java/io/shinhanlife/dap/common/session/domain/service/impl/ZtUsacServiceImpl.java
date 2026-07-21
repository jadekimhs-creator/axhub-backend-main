package io.shinhanlife.dap.common.session.domain.service.impl;

import io.shinhanlife.dap.common.session.domain.repository.ZtUsacRepository;
import io.shinhanlife.dap.common.session.domain.service.ZtUsacService;
import io.shinhanlife.dap.common.session.dto.ZtUsacInDto;
import io.shinhanlife.dap.common.session.dto.ZtUsacOutDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @package io.shinhanlife.dap.common.session.domain.service.impl
 * @className ZtUsacServiceImpl
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