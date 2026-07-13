package io.shinhanlife.axhub.biz.mcp.sample.service;

import io.shinhanlife.axhub.biz.mcp.adapter.support.MciTemplate;
import io.shinhanlife.axhub.biz.mcp.sample.dto.SampleLegacyReqDto;
import io.shinhanlife.axhub.biz.mcp.sample.dto.SampleLegacyResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @package io.shinhanlife.axhub.biz.mcp.sample.service
 * @className SampleLegacyCallService
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
@Slf4j
@Service
@RequiredArgsConstructor
public class SampleLegacyCallService {

    private final MciTemplate mciTemplate;

    /**
     * MCI 인터페이스를 호출하는 샘플 메서드 (MciTemplate 시범 적용)
     * 
     * @param request MCI 호출을 위한 요청 데이터
     * @return MCI 서버의 응답 데이터
     */
    public SampleLegacyResDto callSampleMci(SampleLegacyReqDto request) {
        // 기존 20줄 이상의 통신 로직이 단 1줄로 완벽하게 은닉화 되었습니다.
        return mciTemplate.call(request.getInterfaceId(), request, SampleLegacyResDto.class);
    }
}