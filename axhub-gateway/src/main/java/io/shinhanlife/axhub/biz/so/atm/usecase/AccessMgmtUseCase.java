package io.shinhanlife.axhub.biz.so.atm.usecase;

import io.shinhanlife.axhub.biz.so.atm.dto.RoleListOutDto;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSaveRequest;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSearchRequest;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSearchResponse;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.RoleListRequest;

import java.util.List;

/**
 * @package io.shinhanlife.axhub.biz.so.atm.usecase
 * @className AccessMgmtUseCase
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
public interface AccessMgmtUseCase {

    List<RoleListOutDto> getRoles(RoleListRequest request);

    AthrSearchResponse getAthr(AthrSearchRequest request);

    void saveAthr(AthrSaveRequest request);
}