package io.shinhanlife.axhub.biz.sm.mmg.domain.repository;

import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuInDto;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuOutDto;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuSaveInDto;
import io.shinhanlife.glow.GlowMybatisMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@GlowMybatisMapper
public interface MenuRepository {

    List<MenuOutDto> selectMenu(MenuInDto dto);

    MenuOutDto selectMenuById(@Param("menuId") Integer menuId);

    void insertMenu(MenuSaveInDto dto);

    void updateMenu(MenuSaveInDto dto);

    void deleteMenu(@Param("menuId") Integer menuId);

}