package io.shinhanlife.axhub.sample.usecase;

import io.shinhanlife.axhub.sample.presentation.io.AppliSystNtfyPatiRequest;
import io.shinhanlife.axhub.sample.presentation.io.AppliSystNtfyPatiResponse;
import io.shinhanlife.axhub.sample.presentation.io.StrnTermRequest;
import io.shinhanlife.axhub.sample.presentation.io.StrnTermResponse;

/**
 * @package io.shinhanlife.axhub.sample.usecase
 * @className GlowSampleUseCase
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
public interface GlowSampleUseCase {
    StrnTermResponse getStrnTerms(StrnTermRequest req);
    AppliSystNtfyPatiResponse insertAppliSystNtfyPati(AppliSystNtfyPatiRequest req);
}