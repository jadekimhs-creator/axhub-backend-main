package io.shinhanlife.axhub.biz.so.atm.usecase.impl;

import io.shinhanlife.axhub.biz.so.atm.converter.AccessMgmtConverter;
import io.shinhanlife.axhub.biz.so.atm.domain.service.AccessMgmtService;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleListOutDto;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSaveRequest;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSearchRequest;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSearchResponse;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.RoleListRequest;
import io.shinhanlife.axhub.biz.so.atm.usecase.AccessMgmtUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @package io.shinhanlife.axhub.biz.so.atm.usecase.impl
 * @className AccessMgmtUseCaseImpl
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
public class AccessMgmtUseCaseImpl implements AccessMgmtUseCase {

    private final AccessMgmtService accessMgmtService;
    private final AccessMgmtConverter converter;

    @Override
    public List<RoleListOutDto> getRoles(RoleListRequest request) {
        return accessMgmtService.getRoles(converter.toInDto(request));
    }

    @Override
    public AthrSearchResponse getAthr(AthrSearchRequest request) {
        return converter.toResponse(accessMgmtService.getAthr(converter.toInDto(request)));
    }

    @Override
    public void saveAthr(AthrSaveRequest request) {
        accessMgmtService.saveAthr(converter.toInDto(request));
    }
}