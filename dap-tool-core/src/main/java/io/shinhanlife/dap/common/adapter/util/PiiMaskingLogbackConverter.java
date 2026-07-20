package io.shinhanlife.dap.common.adapter.util;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback 커스텀 컨버터
 * 모든 로그 메시지(%msg)가 파일이나 콘솔에 찍히기 직전에 이 클래스를 거쳐가게 됩니다.
 * 여기서 PiiMaskingUtils.mask()를 호출하여 PII(주민번호, 계좌번호 등)를 안전하게 별표(*) 처리합니다.
 */
/**
 * @package io.shinhanlife.dap.common.adapter.util
 * @className PiiMaskingLogbackConverter
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
public class PiiMaskingLogbackConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        // 원본 로그 메시지를 가져옵니다.
        String originalMessage = super.convert(event);
        
        // 정규식을 이용하여 개인정보가 포함되어 있으면 마스킹 처리하여 반환합니다.
        return PiiMaskingUtils.mask(originalMessage);
    }
}