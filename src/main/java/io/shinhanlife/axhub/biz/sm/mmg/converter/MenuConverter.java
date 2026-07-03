package io.shinhanlife.axhub.biz.sm.mmg.converter;

import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuInDto;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuListDto;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmMmg0000M01RRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmMmg0000M01RResponse;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public abstract class MenuConverter {

    public abstract MenuInDto smMmg0000M01RRequestToMenuInDto(SmMmg0000M01RRequest req);

    public abstract MenuListDto smMmg0000M01RResponseToMenuListDto(SmMmg0000M01RResponse res);

    public abstract SmMmg0000M01RResponse menuListDtoeToSmMmg0000M01RResponse(MenuListDto res);

}