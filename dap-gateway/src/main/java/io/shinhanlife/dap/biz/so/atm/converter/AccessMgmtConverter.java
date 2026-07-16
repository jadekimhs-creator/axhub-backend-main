package io.shinhanlife.dap.biz.so.atm.converter;

import io.shinhanlife.dap.biz.so.atm.dto.AthrOutDto;
import io.shinhanlife.dap.biz.so.atm.dto.AthrSaveInDto;
import io.shinhanlife.dap.biz.so.atm.dto.AthrSearchInDto;
import io.shinhanlife.dap.biz.so.atm.dto.RoleListInDto;
import io.shinhanlife.dap.biz.so.atm.presentation.io.AthrSaveRequest;
import io.shinhanlife.dap.biz.so.atm.presentation.io.AthrSearchRequest;
import io.shinhanlife.dap.biz.so.atm.presentation.io.AthrSearchResponse;
import io.shinhanlife.dap.biz.so.atm.presentation.io.RoleListRequest;
import org.mapstruct.Mapper;

/**
 * @package io.shinhanlife.dap.biz.so.atm.converter
 * @className AccessMgmtConverter
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
@Mapper(componentModel = "spring")
public abstract class AccessMgmtConverter {

    public abstract RoleListInDto toInDto(RoleListRequest request);

    public abstract AthrSearchInDto toInDto(AthrSearchRequest request);

    public abstract AthrSaveInDto toInDto(AthrSaveRequest request);

    public abstract AthrSearchResponse toResponse(AthrOutDto outDto);
}