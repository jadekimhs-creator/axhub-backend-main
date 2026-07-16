package io.shinhanlife.dap.common.session.domain.service;

import io.shinhanlife.dap.common.session.dto.ZtUsacInDto;
import io.shinhanlife.dap.common.session.dto.ZtUsacOutDto;

/**
 * @package io.shinhanlife.dap.common.session.domain.service
 * @className ZtUsacService
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
public interface ZtUsacService {

    /**
     * 사용자 조회 (단건)
     *
     * @param dto 사번
     * @return 인사정보
     */
    ZtUsacOutDto selectZtUsac(ZtUsacInDto dto);
}