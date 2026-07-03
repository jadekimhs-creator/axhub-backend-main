package io.shinhanlife.axhub.biz.sm.mmg.usecase.impl;

import java.util.ArrayList;
import java.util.List;

import io.shinhanlife.axhub.biz.sm.mmg.converter.MenuConverter;
import io.shinhanlife.axhub.biz.sm.mmg.domain.service.impl.MenuService;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuInDto;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuListDto;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmMmg0000M01RRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmMmg0000M01RResponse;
import io.shinhanlife.axhub.biz.sm.mmg.usecase.SmMmg0000MUseCase;
import io.shinhanlife.glow.GlowServiceGroupId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@GlowServiceGroupId(value = "SmMmgc", description="SmMmg")
public class SmMmg0000MUseCaseImpl implements SmMmg0000MUseCase {

    private final MenuService menuService;
    private final MenuConverter converter;

    @Override
    public List<SmMmg0000M01RResponse> selectMenu(SmMmg0000M01RRequest req) {
        List<SmMmg0000M01RResponse> result = new ArrayList<>();
        MenuInDto dto = converter.smMmg0000M01RRequestToMenuInDto(req);
        List<MenuListDto> menuListDtos = menuService.selectMenu(dto);



        for (MenuListDto menuListDto : menuListDtos) {
            result.add(converter.menuListDtoeToSmMmg0000M01RResponse(menuListDto));
        }
        return result;
    }
}
