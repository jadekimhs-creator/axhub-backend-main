package io.shinhanlife.axhub.biz.sm.mmg.presentation;

import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmMmg0000M01RRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmMmg0000M01RResponse;
import io.shinhanlife.axhub.biz.sm.mmg.usecase.SmMmg0000MUseCase;
import io.shinhanlife.glow.BaseResponse;
import io.shinhanlife.glow.GlowControllerId;
import java.util.List;

import io.shinhanlife.glow.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class SmMmg0000MController {

    private final SmMmg0000MUseCase smMmg0000MUseCase;

    @PostMapping("SMMMG0000M01R")
    @GlowControllerId("SMMMG0000M01R")
    public ResponseEntity<BaseResponse<List<SmMmg0000M01RResponse>>> SMMMG0000M01R(@RequestBody SmMmg0000M01RRequest req) {
        return ResponseUtil.ok(smMmg0000MUseCase.selectMenu(req));
    }

}