package io.shinhanlife.axhub.biz.so.atm.domain.service;

import io.shinhanlife.axhub.biz.so.atm.dto.AthrOutDto;
import io.shinhanlife.axhub.biz.so.atm.dto.AthrSaveInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.AthrSearchInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleListInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleListOutDto;

import java.util.List;

/**
 * @package io.shinhanlife.axhub.biz.so.atm.domain.service
 * @className AccessMgmtService
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
public interface AccessMgmtService {

    List<RoleListOutDto> getRoles(RoleListInDto inDto);

    AthrOutDto getAthr(AthrSearchInDto inDto);

    void saveAthr(AthrSaveInDto inDto);
}