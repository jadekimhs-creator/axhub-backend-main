package io.shinhanlife.dap.biz.so.atm.presentation.io;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @package io.shinhanlife.dap.biz.so.atm.presentation.io
 * @className AthrSaveRequest
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
public class AthrSaveRequest {
    private String       systId;
    private String       roleNo;
    private List<String> grantedToolIds;
    private List<String> grantedKnwlIds;
}