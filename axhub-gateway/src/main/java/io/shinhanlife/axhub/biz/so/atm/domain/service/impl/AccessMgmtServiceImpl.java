package io.shinhanlife.axhub.biz.so.atm.domain.service.impl;

import io.shinhanlife.axhub.biz.so.atm.domain.repository.AccessMgmtMapper;
import io.shinhanlife.axhub.biz.so.atm.domain.service.AccessMgmtService;
import io.shinhanlife.axhub.biz.so.atm.dto.AthrItemOutDto;
import io.shinhanlife.axhub.biz.so.atm.dto.AthrOutDto;
import io.shinhanlife.axhub.biz.so.atm.dto.AthrSaveInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.AthrSearchInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleKnwlAthrInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleListInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleListOutDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleToolAthrInDto;
import io.shinhanlife.axhub.common.util.SessionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @package io.shinhanlife.axhub.biz.so.atm.domain.service.impl
 * @className AccessMgmtServiceImpl
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
public class AccessMgmtServiceImpl implements AccessMgmtService {

    private final AccessMgmtMapper accessMgmtMapper;

    private static final String SYSTEM_CD  = "AXH";
    private static final String SYSTEM_PRG = "SOATM0100";

    @Override
    public List<RoleListOutDto> getRoles(RoleListInDto inDto) {
        return accessMgmtMapper.selectRoles(inDto);
    }

    @Override
    public AthrOutDto getAthr(AthrSearchInDto inDto) {
        Set<String> grantedToolIds = accessMgmtMapper.selectGrantedToolIds(inDto)
                .stream().collect(Collectors.toSet());

        Set<String> grantedKnwlIds = accessMgmtMapper.selectGrantedKnwlIds(inDto)
                .stream().collect(Collectors.toSet());

        List<AthrItemOutDto> tools = accessMgmtMapper.selectAllTools()
                .stream()
                .peek(item -> item.setGranted(grantedToolIds.contains(item.getResourceId())))
                .collect(Collectors.toList());

        List<AthrItemOutDto> knwls = accessMgmtMapper.selectAllKnwls()
                .stream()
                .peek(item -> item.setGranted(grantedKnwlIds.contains(item.getResourceId())))
                .collect(Collectors.toList());

        return new AthrOutDto(tools, knwls);
    }

    @Override
    @Transactional
    public void saveAthr(AthrSaveInDto inDto) {
        Date now = new Date();
        String systId = inDto.getSystId();
        String roleNo = inDto.getRoleNo();

        accessMgmtMapper.deleteToolAthr(systId, roleNo);
        if (inDto.getGrantedToolIds() != null) {
            for (String toolId : inDto.getGrantedToolIds()) {
                accessMgmtMapper.insertToolAthr(buildToolAthrInDto(systId, roleNo, toolId, now));
            }
        }

        accessMgmtMapper.deleteKnwlAthr(systId, roleNo);
        if (inDto.getGrantedKnwlIds() != null) {
            for (String knwlId : inDto.getGrantedKnwlIds()) {
                accessMgmtMapper.insertKnwlAthr(buildKnwlAthrInDto(systId, roleNo, knwlId, now));
            }
        }
    }

    private RoleToolAthrInDto buildToolAthrInDto(String systId, String roleNo, String toolId, Date now) {
        RoleToolAthrInDto dto = new RoleToolAthrInDto();
        dto.setRoleToolAthrId("RTA-" + shortUuid());
        dto.setSystId(systId);
        dto.setRoleNo(roleNo);
        dto.setToolId(toolId);
        dto.setPuseYn("Y");
        fillAudit(dto, now);
        return dto;
    }

    private RoleKnwlAthrInDto buildKnwlAthrInDto(String systId, String roleNo, String knwlId, Date now) {
        RoleKnwlAthrInDto dto = new RoleKnwlAthrInDto();
        dto.setRoleKnwlAthrId("RKA-" + shortUuid());
        dto.setSystId(systId);
        dto.setRoleNo(roleNo);
        dto.setKnwlId(knwlId);
        dto.setPuseYn("Y");
        fillAudit(dto, now);
        return dto;
    }

    private void fillAudit(RoleToolAthrInDto dto, Date now) {
        String prafNo = SessionUtil.getPrafNo();
        String ognzNo = SessionUtil.getOgnzNo();
        dto.setSystRgiDt(now);       dto.setSystRgiPrafNo(prafNo);
        dto.setSystRgiOgnzNo(ognzNo); dto.setSystRgiSystCd(SYSTEM_CD);
        dto.setSystRgiPrgrId(SYSTEM_PRG);
        dto.setSystChgDt(now);       dto.setSystChgPrafNo(prafNo);
        dto.setSystChgOgnzNo(ognzNo); dto.setSystChgSystCd(SYSTEM_CD);
        dto.setSystChgPrgrId(SYSTEM_PRG);
    }

    private void fillAudit(RoleKnwlAthrInDto dto, Date now) {
        String prafNo = SessionUtil.getPrafNo();
        String ognzNo = SessionUtil.getOgnzNo();
        dto.setSystRgiDt(now);       dto.setSystRgiPrafNo(prafNo);
        dto.setSystRgiOgnzNo(ognzNo); dto.setSystRgiSystCd(SYSTEM_CD);
        dto.setSystRgiPrgrId(SYSTEM_PRG);
        dto.setSystChgDt(now);       dto.setSystChgPrafNo(prafNo);
        dto.setSystChgOgnzNo(ognzNo); dto.setSystChgSystCd(SYSTEM_CD);
        dto.setSystChgPrgrId(SYSTEM_PRG);
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}