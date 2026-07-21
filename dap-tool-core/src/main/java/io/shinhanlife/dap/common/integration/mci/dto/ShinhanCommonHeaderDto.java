package io.shinhanlife.dap.common.integration.mci.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @package io.shinhanlife.dap.common.integration.mci.dto
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

    private String tgrmLencn;           // 전문길이
    private String glbId;               // 글로벌ID (전사공통키)
    private String pgrsSriaNo;          // 진행일련번호
    private String tgrmVrsnInfoValu;    // 전문버전정보값
    private String tgrmEncrYn;          // 전문암호화여부
    private String gpcpCd;              // 그룹사코드
    private String appliDutjCd;         // 어플리케이션업무코드
    private String appliDtptDutjCd;     // 어플리케이션상세업무코드
    private String frbuCd;              // 대외기관코드
    private String cmouDutjCd;          // 대외업무코드
    private String cmouCssfCd;          // 대외종별코드
    private String cmouTraCd;           // 대외거래코드
    private String rcvSvcId;            // 수신서비스ID
    private String rsltRcvSvcId;        // 결과수신서비스ID
    private String tgrmCreaChnnTypeCd;  // 전문생성채널유형코드

    private String lnggDvsnCd;          // 언어구분코드
    private String simulTraYn;          // 시뮬레이션거래여부
    private String itrIfId;             // 인터페이스ID
    private String reqRspnScCd;         // 요청응답구분코드
    private String tnsmTypeCd;          // 전송유형코드
    private String envrTypeCd;          // 환경유형코드
    private String inqrTraTypeCd;       // 조회거래유형코드
    private String reqTgrmTnsmDtptDt;   // 요청전문전송상세일시
    private String strYmd;              // 기준일자
    private String scrnId;              // 화면ID
    private String scrnBtnId;           // 화면버튼ID

    private String userIpAddr;          // 사용자IP주소
    private String drtmCd;              // 부서코드
    private String userId;              // 사용자ID
    private String indvCtinRoleCd;      // 개인신용정보역할코드
    private String acntOgnzNo;          // 경리조직번호
    private String rspnTgrmTnsmDtptDt;  // 응답전문전송상세일시
    private String tgrmDalRsltCd;       // 전문처리결과코드
    private String ognzAsrtCd;          // 조직분류코드
    private String ognzLeveCd;          // 조직레벨코드
    private String psmrAsrtCd;          // 인사조직분류코드
    private String sbsnRulpAsrtCd;      // 영업규정분류코드
    private String bsduCd;              // 영업지국코드
    private String bsquCd;              // 영업자격코드
    private String linkPrafDutyCd;      // 연계인사직책코드
    private String indvInfoLogWritYn;   // 개인정보로그작성여부
    private String prepImhdNm;          // 예비항목명
}
