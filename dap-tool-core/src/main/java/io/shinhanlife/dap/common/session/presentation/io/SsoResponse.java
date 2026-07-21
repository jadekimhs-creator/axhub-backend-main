package io.shinhanlife.dap.common.session.presentation.io;

import io.shinhanlife.dap.common.session.dto.SessionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @package io.shinhanlife.dap.common.session.presentation.io
 * @className SsoResponse
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
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsoResponse {

    private String retCode;
    private SessionDto userInfo;
    private String redirectUrl;

}