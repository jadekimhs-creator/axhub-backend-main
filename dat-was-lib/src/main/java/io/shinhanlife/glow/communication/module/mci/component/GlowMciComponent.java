package io.shinhanlife.glow.communication.module.mci.component;

import io.shinhanlife.glow.communication.dto.Transfer;
import org.springframework.stereotype.Component;

/**
 * TODO: 실제 Glow Framework 의존성(JAR)이 추가되면 이 Mock 클래스를 삭제하세요.
 */
@Component
public class GlowMciComponent<S, R> {
    
    public Transfer<R> sync(Transfer<S> request) {
        // 실제 Glow HTTP 통신 (GlowHttpHeaderUtil, HttpClient 등) 수행 시뮬레이션
        Object responseBody = new Object();
        if (request != null && request.getResBodyClass() != null) {
            try {
                responseBody = request.getResBodyClass().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // Ignore and fallback to Object
            }
        }
        
        return (Transfer<R>) Transfer.builder()
                .header(request != null ? request.getHeader() : null)
                .body(responseBody)
                .build();
    }
}
