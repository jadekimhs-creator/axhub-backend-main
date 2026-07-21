package io.shinhanlife.dap.mcc.component;

import io.shinhanlife.glow.communication.dto.Transfer;
import io.shinhanlife.glow.communication.module.mci.component.GlowMciComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 신한라이프 내부 Glow 표준 컴포넌트 어댑터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AxhubMciComponent {
    
    @SuppressWarnings("rawtypes")
    private final GlowMciComponent mci;

    @SuppressWarnings("unchecked")
    public <O, I> Transfer<O> callTo(String itrfName, String rcvSvcId, I inputDto, Class<O> resBodyClass) {
        log.info("[AxhubMciComponent] MCI 호출 준비 - 인터페이스: {}, 수신서비스: {}", itrfName, rcvSvcId);
        
        // Header 세팅 로직 생략 (Mock)
        
        Transfer<Object> request = Transfer.builder()
                .body(inputDto)
                .resBodyClass((Class<Object>) (Class<?>) resBodyClass)
                .build();
                
        return syncMci(request);
    }

    @SuppressWarnings("unchecked")
    private <O> Transfer<O> syncMci(Transfer<Object> request) {
        log.info("[AxhubMciComponent] GlowMciComponent.sync() 호출");
        return (Transfer<O>) mci.sync(request);
    }
}
