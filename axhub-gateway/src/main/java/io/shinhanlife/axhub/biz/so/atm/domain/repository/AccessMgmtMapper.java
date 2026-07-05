package io.shinhanlife.axhub.biz.so.atm.domain.repository;

import io.shinhanlife.axhub.biz.so.atm.dto.AthrItemOutDto;
import io.shinhanlife.axhub.biz.so.atm.dto.AthrSearchInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleKnwlAthrInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleListInDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleListOutDto;
import io.shinhanlife.axhub.biz.so.atm.dto.RoleToolAthrInDto;
import io.shinhanlife.glow.GlowMybatisMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@GlowMybatisMapper
public interface AccessMgmtMapper {

    List<RoleListOutDto> selectRoles(RoleListInDto inDto);

    List<AthrItemOutDto> selectAllTools();

    List<AthrItemOutDto> selectAllKnwls();

    List<String> selectGrantedToolIds(AthrSearchInDto inDto);

    List<String> selectGrantedKnwlIds(AthrSearchInDto inDto);

    void deleteToolAthr(@Param("systId") String systId, @Param("roleNo") String roleNo);

    void insertToolAthr(RoleToolAthrInDto inDto);

    void deleteKnwlAthr(@Param("systId") String systId, @Param("roleNo") String roleNo);

    void insertKnwlAthr(RoleKnwlAthrInDto inDto);
}
