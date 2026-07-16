package io.shinhanlife.axhub.common.integration.mci.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @package io.shinhanlife.axhub.common.integration.mci.dto
 * @className ShinhanCommonHeaderDto
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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShinhanCommonHeaderDto {

    private String glbId;               // 전사공통키 (37 Byte)
    private String pgrsSriaNo;          // 진행일련번호
    private String trgmVrsnInfoValu;    // 전문버전정보값
    private String tgrmEncrYn;          // 전문암호화여부
    private String gpcpCd;              // 글로벌법인코드
    private String appliDutjCd;         // 어플리케이션업무코드
    private String rcvSvcId;            // 수신서비스ID
    private String reqRspnScCd;         // 요청응답구분코드 (S:요청, R:응답)
    private String inqrTraTypeCd;       // 조회거래유형코드
    private String reqTgrmTnsmDtptDt;   // 요청전문전송일시
    private String itrIfId;             // 인터페이스ID

    // 대외 연계 정보 (선택)
    private String frbuCd;              // 대외기관코드
    private String cmouDutjCd;          // 대외업무코드
    private String cmouCssfCd;          // 대외종별코드
    private String cmouTraCd;           // 대외거래코드
}
