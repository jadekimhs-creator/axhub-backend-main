package io.shinhanlife.axhub.biz.sm.mmg.presentation;

import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01DRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01RRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01RResponse;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01SRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01SResponse;
import io.shinhanlife.axhub.biz.sm.mmg.usecase.SmNmg0100MUseCase;
import io.shinhanlife.glow.BaseResponse;
import io.shinhanlife.glow.GlowControllerId;
import io.shinhanlife.glow.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SmNmg0100MController {

    private final SmNmg0100MUseCase useCase;

    @PostMapping("SMNMG0100M01R")
    @GlowControllerId("SMNMG0100M01R")
    public ResponseEntity<BaseResponse<List<SmNmg0100M01RResponse>>> SMNMG0100M01R(
            @RequestBody SmNmg0100M01RRequest req) {
        return ResponseUtil.ok(useCase.selectMenu(req));
    }

    @PostMapping("SMNMG0100M01S")
    @GlowControllerId("SMNMG0100M01S")
    public ResponseEntity<BaseResponse<SmNmg0100M01SResponse>> SMNMG0100M01S(
            @RequestBody SmNmg0100M01SRequest req) {
        return ResponseUtil.ok(useCase.saveMenu(req));
    }

    @PostMapping("SMNMG0100M01D")
    @GlowControllerId("SMNMG0100M01D")
    public ResponseEntity<BaseResponse<Void>> SMNMG0100M01D(
            @RequestBody SmNmg0100M01DRequest req) {
        useCase.deleteMenu(req);
        return ResponseUtil.ok(null);
    }
}
