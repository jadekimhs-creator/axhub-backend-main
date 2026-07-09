package io.shinhanlife.axhub.common.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// TODO: 실제 Glow Framework 의존성이 추가되면 아래 주석들을 풀고 사용하세요!
// import io.shinhanlife.glow.communication.dto.CommonHeader;
// import io.shinhanlife.glow.communication.dto.Transfer;
// import io.shinhanlife.glow.communication.module.eai.component.GlowEaiComponent;
// import io.shinhanlife.glow.communication.module.mci.component.GlowExtMciComponent;
// import io.shinhanlife.glow.communication.module.mci.component.GlowMciComponent;
// import io.shinhanlife.glow.communication.util.CommonHeaderFactory;

/**
 * [MCI / EAI 공통 연동 래퍼(Wrapper) 템플릿]
 * 신한라이프 Glow Framework 개발표준정의서를 바탕으로 대내/대외망/EAI 통신을 
 * MCP 툴에서 손쉽게 호출할 수 있도록 일원화한 컴포넌트입니다.
 */
/**
 * @package io.shinhanlife.axhub.common.integration
 * @className GlowIntegrationCall
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
@Component
@RequiredArgsConstructor
public class GlowIntegrationCall {

    // 1. 실제 의존성이 주입될 프레임워크 컴포넌트들 (임시 주석 처리)
    /*
    private final GlowMciComponent mci;
    private final GlowEaiComponent eai;
    private final GlowExtMciComponent extMci;
    */

    /**
     * 1. 대외 MCI 호출 (타행, 금융결제원 등 외부 기관)
     * 가이드 2.2.2에 명시된 필수 파라미터(기관코드, 종별코드, 업무코드, 거래코드)를 모두 포함합니다.
     */
    /*
    public <S, R> Transfer<R> callExtMci(String itrfId, String frbuCd, String cmouDutjCd, String cmouCssfCd, String cmouTraCd, S body, Class<R> resBody) {
        
        // 1. 공통 헤더 생성
        CommonHeader header = CommonHeaderFactory.createRequestHeader(itrfId);
        
        // 2. 대외 전용 필수 코드 세팅 로직 (프레임워크 내부 스펙에 맞게 가공)
        // (예: 헤더에 해당 속성들을 주입하거나 Transfer 객체에 싣는 과정 추가)
        
        // 3. Transfer 객체 빌드
        Transfer<S> req = Transfer.<S>builder()
                .header(header)
                .body(body)
                .build();
                
        // 4. 대외 MCI 컴포넌트를 통해 최종 전송
        return extMci.call(req, resBody);
    }
    */

    /**
     * 2. EAI 호출 (대내망 중계기)
     * 가이드 2.3에 명시된 대로 수신서비스 ID 없이 인터페이스 ID(itrfId)만 필수로 받습니다.
     */
    /*
    public <S, R> Transfer<R> callEai(String itrfId, S body, Class<R> resBody) {
        
        // 1. EAI는 인터페이스 ID만으로 심플하게 헤더 생성
        CommonHeader header = CommonHeaderFactory.createRequestHeader(itrfId);
        
        // 2. Transfer 객체 빌드
        Transfer<S> req = Transfer.<S>builder()
                .header(header)
                .body(body)
                .build();
                
        // 3. EAI 컴포넌트를 통해 최종 전송
        return eai.call(req, resBody);
    }
    */

    /**
     * 3. 대내 MCI 호출 (사내 시스템 간 통신)
     */
    /*
    public <S, R> Transfer<R> callMci(String itrfId, S body, Class<R> resBody) {
        
        CommonHeader header = CommonHeaderFactory.createRequestHeader(itrfId);
        
        Transfer<S> req = Transfer.<S>builder()
                .header(header)
                .body(body)
                .build();
                
        return mci.call(req, resBody);
    }
    */
}