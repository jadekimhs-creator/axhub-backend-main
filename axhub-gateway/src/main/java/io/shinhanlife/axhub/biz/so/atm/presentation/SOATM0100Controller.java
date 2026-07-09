package io.shinhanlife.axhub.biz.so.atm.presentation;

import io.shinhanlife.axhub.biz.so.atm.dto.RoleListOutDto;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSaveRequest;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSearchRequest;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.AthrSearchResponse;
import io.shinhanlife.axhub.biz.so.atm.presentation.io.RoleListRequest;
import io.shinhanlife.axhub.biz.so.atm.usecase.AccessMgmtUseCase;
import io.shinhanlife.glow.BaseResponse;
import io.shinhanlife.glow.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @package io.shinhanlife.axhub.biz.so.atm.presentation
 * @className SOATM0100Controller
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
@RequestMapping("/so/atm")
@RequiredArgsConstructor
public class SOATM0100Controller {

    private final AccessMgmtUseCase accessMgmtUseCase;

    @PostMapping("/SOATM0100R")
    public ResponseEntity<BaseResponse<List<RoleListOutDto>>> getRoles(
            @RequestBody RoleListRequest request) {
        return ResponseUtil.ok(accessMgmtUseCase.getRoles(request));
    }

    @PostMapping("/SOATM0101R")
    public ResponseEntity<BaseResponse<AthrSearchResponse>> getAthr(
            @RequestBody AthrSearchRequest request) {
        return ResponseUtil.ok(accessMgmtUseCase.getAthr(request));
    }

    @PostMapping("/SOATM0100U")
    public ResponseEntity<BaseResponse<Void>> saveAthr(
            @RequestBody AthrSaveRequest request) {
        accessMgmtUseCase.saveAthr(request);
        return ResponseUtil.ok();
    }
}