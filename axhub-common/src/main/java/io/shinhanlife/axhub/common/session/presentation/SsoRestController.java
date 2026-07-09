package io.shinhanlife.axhub.common.session.presentation;

import io.micrometer.common.util.StringUtils;
import io.shinhanlife.glow.BaseResponse;
import io.shinhanlife.glow.BizException;
import io.shinhanlife.glow.GlowControllerId;
import io.shinhanlife.glow.ResponseUtil;
import io.shinhanlife.axhub.common.session.converter.ZtUsacConverter;
import io.shinhanlife.axhub.common.session.domain.service.ZtUsacService;
import io.shinhanlife.axhub.common.session.dto.SessionDto;
import io.shinhanlife.axhub.common.session.dto.ZtUsacInDto;
import io.shinhanlife.axhub.common.session.dto.ZtUsacOutDto;
import io.shinhanlife.axhub.common.session.presentation.io.SsoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * @package io.shinhanlife.axhub.common.session.presentation
 * @className SsoRestController
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
@Slf4j
@RequestMapping("/sso")
public class SsoRestController {

    private static final String NLS_LOGIN_URL = "";
    private final ZtUsacService ztUsacService;
    private final ZtUsacConverter ztUsacConverter;

    /**
     * sso 연동 전 임시 로그인
     *
     * @param request
     * @param response
     * @param session
     * @param <T>
     * @return
     */
    @GlowControllerId("tempLogin")
    @PostMapping("/tempLogin")
    public <T> ResponseEntity<BaseResponse<SsoResponse>> tempLogin(HttpServletRequest request, HttpServletResponse response,
                                                                   HttpSession session, @RequestBody SessionDto requestDto) {
        try {
            if (StringUtils.isEmpty(requestDto.getPrafNo())) {
                throw new NotFoundException("SSO >> not found sso id");
            }

            // DB 유저 가져오기
            ZtUsacOutDto ztUsacOutDto = ztUsacService.selectZtUsac(ZtUsacInDto.builder().prafNo(requestDto.getPrafNo()).puseYn("Y")
                                                                           .build());
            if (Objects.isNull(ztUsacOutDto) || StringUtils.isEmpty(ztUsacOutDto.getPrafNo())) {
                throw new BizException("SSO >> not found UserInfo >> retCode:");
            }

            SessionDto sessionDto = ztUsacConverter.toSessionDto(ztUsacOutDto);
            sessionDto.setLoginDtm(); // 로그인 시점 세팅
            session.setAttribute("userInfo", sessionDto);
            return ResponseUtil.ok(SsoResponse.builder().retCode("0").userInfo(sessionDto).build());

        } catch (Exception e) {
            log.error("로그인 실패", e);
            session.invalidate();
        }

        return ResponseUtil.ok(SsoResponse.builder().redirectUrl(NLS_LOGIN_URL).build());

    }

}