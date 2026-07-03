package io.shinhanlife.axhub.biz.sm.mmg.domain.service.impl;


import io.shinhanlife.axhub.biz.sm.mmg.domain.repository.MenuRepository;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuInDto;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuListDto;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuOutDto;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuSaveInDto;
import io.shinhanlife.axhub.common.util.SessionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;

    @Override
    public List<MenuListDto> selectMenu(MenuInDto dto) {
        List<MenuOutDto> allMenus = menuRepository.selectMenu(dto);

        // 비루트 메뉴(부모 ID가 null이 아닌)만 그룹핑하여 맵 생성
        Map<Integer, List<MenuOutDto>> menuMap = allMenus.stream()
            .filter(menu -> menu.getParentMenuId() != null)
            .collect(Collectors.groupingBy(MenuOutDto::getParentMenuId));

        // 루트 메뉴 (parentMenuId == null)
        List<MenuOutDto> roots = allMenus.stream()
            .filter(menu -> menu.getParentMenuId() == null)
            .collect(Collectors.toList());

        // 트리 빌드
        List<MenuListDto> menuList = buildTree(roots, menuMap);

        return menuList;
    }

    private List<MenuListDto> buildTree(List<MenuOutDto> menus, Map<Integer, List<MenuOutDto>> menuMap) {
        List<MenuListDto> dtos = new ArrayList<>();
        for (MenuOutDto menu : menus) {
            MenuListDto dto = convertToDto(menu);
            List<MenuOutDto> children = menuMap.getOrDefault(menu.getMenuId(), new ArrayList<>());
            if (!children.isEmpty()) {
                dto.setChildren(buildTree(children, menuMap));
            }
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public MenuOutDto findById(Integer menuId) {
        return menuRepository.selectMenuById(menuId);
    }

    @Override
    @Transactional
    public Integer save(MenuSaveInDto dto) {
        Date now = new Date();
        String prafNo = SessionUtil.getPrafNo();
        String ognzNo = SessionUtil.getOgnzNo();

        if (dto.getIsActive() == null) dto.setIsActive("Y");
        if (dto.getIsHide() == null) dto.setIsHide("N");
        if (dto.getIsHideTab() == null) dto.setIsHideTab("N");
        if (dto.getIsIframe() == null) dto.setIsIframe("N");
        if (dto.getKeepAlive() == null) dto.setKeepAlive("N");
        if (dto.getOrderSeq() == null) dto.setOrderSeq(0);

        dto.setSystChgDt(now);
        dto.setSystChgPrafNo(prafNo);
        dto.setSystChgOgnzNo(ognzNo);
        dto.setSystChgSystCd("AXH");
        dto.setSystChgPrgrId("SMNMG0100M");

        if (dto.getMenuId() == null) {
            dto.setSystRgiDt(now);
            dto.setSystRgiPrafNo(prafNo);
            dto.setSystRgiOgnzNo(ognzNo);
            dto.setSystRgiSystCd("AXH");
            dto.setSystRgiPrgrId("SMNMG0100M");
            menuRepository.insertMenu(dto);
        } else {
            menuRepository.updateMenu(dto);
        }
        return dto.getMenuId();
    }

    @Override
    @Transactional
    public void delete(Integer menuId) {
        menuRepository.deleteMenu(menuId);
    }

    private MenuListDto convertToDto(MenuOutDto menu) {
        MenuListDto.Meta meta = new MenuListDto.Meta();
        meta.setTitle(menu.getTitle());
        meta.setIcon(menu.getIcon());
        meta.setHide("Y".equals(menu.getIsHide()));
        meta.setHideTab("Y".equals(menu.getIsHideTab()));
        meta.setLink(menu.getLink());
        meta.setIframe("Y".equals(menu.getIsIframe()));
        meta.setKeepAlive("Y".equals(menu.getKeepAlive()));
        meta.setOrderSeq(menu.getOrderSeq());
        meta.setActive("Y".equals(menu.getIsActive()));
        return MenuListDto.builder().id(menu.getMenuId()).name(menu.getName()).path(menu.getPath()).meta(meta).build();
    }

}
