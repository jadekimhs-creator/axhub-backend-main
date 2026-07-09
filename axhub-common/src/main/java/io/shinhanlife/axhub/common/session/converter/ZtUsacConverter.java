package io.shinhanlife.axhub.common.session.converter;

import io.shinhanlife.axhub.common.session.dto.SessionDto;
import io.shinhanlife.axhub.common.session.dto.ZtUsacOutDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @package io.shinhanlife.axhub.common.session.converter
 * @className ZtUsacConverter
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
public abstract class ZtUsacConverter {

    @Mapping(target = "loginDtm", ignore = true)
    @Mapping(target = "isManager", ignore = true)
    public abstract SessionDto toSessionDto(ZtUsacOutDto dto);

}