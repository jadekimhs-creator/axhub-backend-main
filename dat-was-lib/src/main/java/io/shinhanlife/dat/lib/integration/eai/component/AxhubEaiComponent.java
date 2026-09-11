package io.shinhanlife.dat.lib.integration.eai.component;

import io.shinhanlife.dat.lib.config.GlowCommunicationProperties;
import io.shinhanlife.glow.communication.dto.CommonHeader;
import io.shinhanlife.glow.communication.dto.Transfer;
import io.shinhanlife.glow.communication.module.eai.component.GlowEaiComponent;
import io.shinhanlife.glow.communication.util.CommonHeaderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 신한라이프 내부 Glow 표준 EAI 컴포넌트 어댑터 (AXHUB)
 */
@Slf4j
@Component
@RequiredArgsConstructor

/**
 * @package io.shinhanlife.dat.lib.integration.eai.component
 * @className AxhubEaiComponent
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 * 
 * </pre>
 */
public class AxhubEaiComponent {
    
    @SuppressWarnings("rawtypes")
    private final GlowEaiComponent eai;
    private final GlowCommunicationProperties communicationProperties;

    @SuppressWarnings("unchecked")
    private <O> Transfer<O> syncEai(Transfer<Object> request) {
        // LOG 저장 (AXHUB 방식 로깅)
        CommonHeader reqHeader = (CommonHeader) request.getHeader();
        log.info("[AxhubEaiComponent] {} EAI 호출시작 (수신서비스: {})", reqHeader.getItrfId(), reqHeader.getRcvSvcId());
        
        Transfer<O> response = (Transfer<O>) eai.sync(request);
        
        log.info("[AxhubEaiComponent] {} EAI 호출종료 (수신서비스: {})", reqHeader.getItrfId(), reqHeader.getRcvSvcId());

        return response;
    }

    /**
     * EAI 호출
     */
    public <O, I> Transfer<O> call(String itrfId, String rcvSvcId, I inputDto) throws Exception {
        CommonHeader header = CommonHeaderFactory.createRequestHeader(itrfId, rcvSvcId);

        Transfer<Object> request = Transfer.builder()
                .header(header)
                .body(inputDto)
                .build();

        return syncEai(request);
    }

    /**
     * EAI 호출 (응답 타입 명시)
     */
    @SuppressWarnings("unchecked")
    public <O, I> Transfer<O> call(String itrfId, String rcvSvcId, I inputDto, Class<O> resBodyClass) throws Exception {
        CommonHeader header = CommonHeaderFactory.createRequestHeader(itrfId, rcvSvcId);

        Transfer<Object> request = Transfer.builder()
                .header(header)
                .body(inputDto)
                .resBodyClass((Class<Object>) (Class<?>) resBodyClass)
                .build();
                
        return syncEai(request);
    }

    /**
     * EAI 호출 (rcvSvcId 없는 경우)
     */
    public <O, I> Transfer<O> call(String itrfId, I inputDTO, Class<O> resBodyClass) throws Exception {
        String className = inputDTO.getClass().getSimpleName();
        String rcvSvcId = className.replace("_I", "");
        return call(itrfId, rcvSvcId, inputDTO, resBodyClass);
    }

    /**
     * EAI 호출 (Response body class와 rcvSvcId 없는 경우)
     */
    public <O, I> Transfer<O> call(String itrfId, I inputDTO) throws Exception {
        String className = inputDTO.getClass().getSimpleName();
        String rcvSvcId = className.replace("_I", "");
        return call(itrfId, rcvSvcId, inputDTO);
    }
}
