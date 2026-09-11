package io.shinhanlife.dat.lib.integration.mci.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @package io.shinhanlife.dat.lib.integration.mci.dto
 * @className SlCommonHeaderDto
 * @description AX HUB 시스템 처리 클래스 - SL(신한라이프) 표준 헤더
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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlCommonHeaderDto {
    private String length;          // 전문길이
    private SlGlobalId globalId;    // 글로벌ID
    private String headerVer;       // 전문헤더버전
    private String encodeFlag;      // 전문암호화여부

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlGlobalId {
        private String writeDate;   // 전문작성일 (8)
        private String sysCd;       // 생성시스템명 (8)
        private String typeCd;      // 구분코드 (2)
        private String detailCd;    // 세부업무코드 (4)
        private String seqNo;       // 채번번호 (8)
        private String step;        // 진행상황 (2)
    }

    private String groupCoCd;       // 그룹사코드
    private String instCd;          // 기관코드
    private String applCd;          // 업무코드
    private String kindCd;          // 종별코드
    private String txCd;            // 거래코드
    private String pfmAppName;      // 어플리케이션 명
    private String pfmSvcName;      // 서비스 명
    private String pfmFnName;       // 오퍼레이션 명
    private String systemCd;        // 생성시스템구분
    private String trFlag;          // 요청응답구분
    private String syncFlag;        // 동기구분
    private String envrFlag;        // 환경구분
    private String crudFlag;        // 조회거래구분
    private String sendTime;        // 전문전송일시
    private String screenId;        // 화면ID
    private String clntIp;          // Client IP
    private String orgCd;           // 부서(지점)코드
    private String userId;          // 사용자 사번(아이디)
    private String indvCrdtInfo;    // 개인신용정보역할코드
    private String acntOgnzNo;      // 경리조직번호
    private String ttiFlag;         // TimeOut사용
    private String ttiStartTm;      // 최초시작시간
    private String ttiKeepTm;       // 유지시간초수
    private String outMsgTm;        // 응답전문작성일시
    private String resType;         // 처리결과
    private String resCode;         // 응답코드
    private String resBascMsg;      // 응답기본내역
    private String msgType;         // 메시지 유형
    private String rcvSvcCd;        // 수신 서비스 Code
    private String rsltRcvSvcCd;    // 결과수신 서비스 Code
    private String realSvcCd;       // Real 서비스 Code
    private String ognzAsrtCd;      // 조직분류코드
    private String ognzLeveCd;      // 조직레벨구분코드
    private String psmrAsrtCd;      // 인사조직분류코드
    private String sbsnRulpAsrtCd;  // 영업규정분류코드
    private String bsduCd;          // 영업지국코드
    private String bsquCd;          // 영업자격코드
    private String linkPrafDutyCd;  // 직책코드
    private String temp;            // 예비 필드
}
