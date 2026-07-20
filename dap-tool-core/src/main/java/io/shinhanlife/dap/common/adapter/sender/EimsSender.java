package io.shinhanlife.dap.common.adapter.sender;

/**
 * @package io.shinhanlife.dap.common.adapter.sender
 * @className EimsSender
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
public interface EimsSender {
    // 프로토콜에 상관없이 이 메서드 하나로 통일합니다.
    String send(String interfaceId, String payload) throws Exception;
}