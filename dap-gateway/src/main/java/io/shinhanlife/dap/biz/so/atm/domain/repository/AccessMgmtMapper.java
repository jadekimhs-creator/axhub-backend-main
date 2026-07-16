package io.shinhanlife.dap.biz.so.atm.domain.repository;

import io.shinhanlife.dap.biz.so.atm.dto.AthrItemOutDto;
import io.shinhanlife.dap.biz.so.atm.dto.AthrSearchInDto;
import io.shinhanlife.dap.biz.so.atm.dto.RoleKnwlAthrInDto;
import io.shinhanlife.dap.biz.so.atm.dto.RoleListInDto;
import io.shinhanlife.dap.biz.so.atm.dto.RoleListOutDto;
import io.shinhanlife.dap.biz.so.atm.dto.RoleToolAthrInDto;
import io.shinhanlife.glow.GlowMybatisMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @package io.shinhanlife.dap.biz.so.atm.domain.repository
 * @className AccessMgmtMapper
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