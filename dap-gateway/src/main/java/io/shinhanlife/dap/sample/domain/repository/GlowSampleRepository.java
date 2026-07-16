package io.shinhanlife.dap.sample.domain.repository;

import io.shinhanlife.dap.sample.domain.model.AppliSystNtfyPatiModel;
import io.shinhanlife.dap.sample.dto.StrnTermListInDTO;
import io.shinhanlife.dap.sample.dto.StrnTermListOutDTO;
import io.shinhanlife.glow.GlowIndexPaging;
import io.shinhanlife.glow.GlowMybatisMapper;
import io.shinhanlife.glow.PageInfo;

import java.util.List;

/**
 * @package io.shinhanlife.dap.sample.domain.repository
 * @className GlowSampleRepository
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
public interface GlowSampleRepository {

    @GlowIndexPaging
    List<StrnTermListOutDTO.StrnTerm> selectStrnTerm(StrnTermListInDTO inDto, PageInfo pageInfo);

    int insertAppliSystNtfyPati(AppliSystNtfyPatiModel appliSystNtfyPatiModel);

}