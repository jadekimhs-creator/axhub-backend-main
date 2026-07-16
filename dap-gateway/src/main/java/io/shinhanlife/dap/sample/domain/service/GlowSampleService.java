package io.shinhanlife.dap.sample.domain.service;

import io.shinhanlife.dap.sample.dto.AppliSystNtfyRgiInDTO;
import io.shinhanlife.dap.sample.dto.StrnTermListInDTO;
import io.shinhanlife.dap.sample.dto.StrnTermListOutDTO;
import io.shinhanlife.glow.PageInfo;

/**
 * @package io.shinhanlife.dap.sample.domain.service
 * @className GlowSampleService
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
public interface GlowSampleService {
    StrnTermListOutDTO getStrnTerms(StrnTermListInDTO inDto, PageInfo pageInfo);
    int insertAppliSystNtfyPati(AppliSystNtfyRgiInDTO inDto);
}