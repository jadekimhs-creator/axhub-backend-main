package io.shinhanlife.dat.lib.integration.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

// TODO: 실제 Glow Framework 의존성이 추가되면 아래 주석을 풀고 사용하세요!
// import io.shinhanlife.glow.communication.annotation.GlowMciFieldInfo;

/**
 * [대외 MCI 연동용 DTO 표준 템플릿]
 * Glow Framework 개발표준정의서(2.2.1 IO 작성) 규칙을 100% 준수한 샘플입니다.
 * 새로운 대외 통신 전문을 만들 때 이 파일을 복사해서 필드명과 길이만 수정하여 사용하세요.
 */
/**
 * @package io.shinhanlife.dat.lib.integration.dto
 * @className SampleGlowMessage
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
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC) // [규칙 1] Reflection을 위한 기본 생성자 필수 (public 유지)
public class SampleGlowMessage {

    // [규칙 2] @GlowMciFieldInfo 선언 필수 (order: 순서)
    // @GlowMciFieldInfo(order = 1)
    private MessageHeader header;

    // [규칙 2] @GlowMciFieldInfo 선언 필수 (order: 순서)
    // @GlowMciFieldInfo(order = 2)
    private List<MessageBody> msgDtdvValu; // 다건(List) 본문 데이터

    
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PUBLIC)
    public static class MessageHeader {
        
        // [규칙 2] 단건 필드의 경우 length 필수 입력 (EIMS 길이와 일치해야 함)
        // @GlowMciFieldInfo(order = 1, length = 1)
        private String msgTnsmTypeCd;
        
        // @GlowMciFieldInfo(order = 2, length = 8)
        private int msdvLen;
        
        // [규칙 3] 다건(List) 건수 필드의 경우, target 속성에 대상 변수명("msgDtdvValu") 필수 기입!
        // @GlowMciFieldInfo(order = 3, length = 2, target = "msgDtdvValu")
        private int msgRpttCc;
    }


    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PUBLIC)
    public static class MessageBody {
        
        // @GlowMciFieldInfo(order = 1, length = 8)
        private String msgCd;
        
        // @GlowMciFieldInfo(order = 2, length = 1)
        private String msgPrnAttrCd;
        
        // @GlowMciFieldInfo(order = 3, length = 200)
        private String msgCt;
        
        // @GlowMciFieldInfo(order = 4, length = 200)
        private String anxMsgCt;
    }
}