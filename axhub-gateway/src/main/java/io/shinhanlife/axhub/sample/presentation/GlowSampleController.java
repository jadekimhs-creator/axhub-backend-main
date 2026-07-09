package io.shinhanlife.axhub.sample.presentation;


import io.shinhanlife.axhub.sample.presentation.io.AppliSystNtfyPatiRequest;
import io.shinhanlife.axhub.sample.presentation.io.AppliSystNtfyPatiResponse;
import io.shinhanlife.axhub.sample.presentation.io.StrnTermRequest;
import io.shinhanlife.axhub.sample.presentation.io.StrnTermResponse;
import io.shinhanlife.axhub.sample.usecase.GlowSampleUseCase;
import io.shinhanlife.glow.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @package io.shinhanlife.axhub.sample.presentation
 * @className GlowSampleController
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
@RestController
@RequiredArgsConstructor

public class GlowSampleController {
    @GlowLogTarget({GlowLogTarget.Target.CONSOLE, GlowLogTarget.Target.FILE})
    private final GlowLogger log;
    private final GlowSampleUseCase glowSampleUseCase;

    @GlowControllerId(value = "selectStrnTerm")
    @PostMapping("/selectStrnTerm")
    public ResponseEntity<BaseResponse<StrnTermResponse>> getStrnTerms(@RequestBody StrnTermRequest request) {
        return ResponseUtil.ok(glowSampleUseCase.getStrnTerms(request));
    }


    @GlowControllerId(value = "insertAp")
    @PostMapping("/insertAppliSystNtfyPati")
    public ResponseEntity<BaseResponse<AppliSystNtfyPatiResponse>> insertAppliSystNtfyPati(@RequestBody AppliSystNtfyPatiRequest request) {
        return ResponseUtil.ok(glowSampleUseCase.insertAppliSystNtfyPati(request));
    }


}