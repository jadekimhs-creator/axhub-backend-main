package io.shinhanlife.axhub.biz.sm.mmg.usecase.impl;

import io.shinhanlife.axhub.biz.sm.mmg.domain.service.impl.MenuService;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuInDto;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuListDto;
import io.shinhanlife.axhub.biz.sm.mmg.dto.MenuSaveInDto;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01DRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01RRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01RResponse;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01SRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01SResponse;
import io.shinhanlife.axhub.biz.sm.mmg.usecase.SmNmg0100MUseCase;
import io.shinhanlife.glow.GlowServiceGroupId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@GlowServiceGroupId(value = "SmNmgc", description = "SmNmg")
public class SmNmg0100MUseCaseImpl implements SmNmg0100MUseCase {

    private final MenuService menuService;

    @Override
    public List<SmNmg0100M01RResponse> selectMenu(SmNmg0100M01RRequest req) {
        MenuInDto inDto = new MenuInDto();
        inDto.setIsActive(req.getIsActive());
        inDto.setPath(req.getPath());
        inDto.setTitle(req.getTitle());

        List<MenuListDto> tree = menuService.selectMenu(inDto);
        return tree.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public SmNmg0100M01SResponse saveMenu(SmNmg0100M01SRequest req) {
        MenuSaveInDto dto = new MenuSaveInDto();
        dto.setMenuId(req.getMenuId());
        dto.setParentMenuId(req.getParentMenuId());
        dto.setName(req.getName());
        dto.setPath(req.getPath());
        dto.setTitle(req.getTitle());
        dto.setIcon(req.getIcon());
        dto.setIsHide(req.getIsHide());
        dto.setIsHideTab(req.getIsHideTab());
        dto.setLink(req.getLink());
        dto.setIsIframe(req.getIsIframe());
        dto.setKeepAlive(req.getKeepAlive());
        dto.setOrderSeq(req.getOrderSeq());
        dto.setIsActive(req.getIsActive());

        Integer savedId = menuService.save(dto);
        return SmNmg0100M01SResponse.builder().menuId(savedId).build();
    }

    @Override
    public void deleteMenu(SmNmg0100M01DRequest req) {
        menuService.delete(req.getMenuId());
    }

    private SmNmg0100M01RResponse toResponse(MenuListDto dto) {
        MenuListDto.Meta m = dto.getMeta();

        SmNmg0100M01RResponse.Meta meta = SmNmg0100M01RResponse.Meta.builder()
                .title(m != null ? m.getTitle() : null)
                .icon(m != null ? m.getIcon() : null)
                .hide(m != null && m.isHide())
                .hideTab(m != null && m.isHideTab())
                .link(m != null ? m.getLink() : null)
                .iframe(m != null && m.isIframe())
                .keepAlive(m != null && m.isKeepAlive())
                .orderSeq(m != null ? m.getOrderSeq() : 0)
                .active(m != null && m.isActive())
                .build();

        List<SmNmg0100M01RResponse> children = (dto.getChildren() == null || dto.getChildren().isEmpty())
                ? Collections.emptyList()
                : dto.getChildren().stream().map(this::toResponse).collect(Collectors.toList());

        return SmNmg0100M01RResponse.builder()
                .id(dto.getId())
                .parentMenuId(dto.getParentMenuId())
                .name(dto.getName())
                .path(dto.getPath())
                .meta(meta)
                .children(children)
                .build();
    }
}