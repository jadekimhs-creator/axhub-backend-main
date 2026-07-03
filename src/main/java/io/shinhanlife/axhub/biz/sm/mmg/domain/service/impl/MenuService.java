package io.shinhanlife.axhub.biz.sm.mmg.domain.service.impl;


import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuInDto;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuListDto;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuOutDto;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuSaveInDto;

import java.util.List;

public interface MenuService {

    List<MenuListDto> selectMenu(MenuInDto dto);

    MenuOutDto findById(Integer menuId);

    Integer save(MenuSaveInDto dto);

    void delete(Integer menuId);
}