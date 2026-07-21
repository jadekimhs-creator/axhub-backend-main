package io.shinhanlife.dap.common.integration.mci.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * @package io.shinhanlife.dap.common.integration.mci.dto
 * @className OlCommonHeaderDto
 * @description AX HUB 시스템 처리 클래스 - OL(구 오렌지라이프) 공통 헤더
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
public class OlCommonHeaderDto {
    private String custNm;                  // 고객명
    private String custRrn;                 // 고객 주민등록번호
    private String custNo;                  // 고객번호
    private String rcevNo;                  // 접수번호
    private String pono;                    // 증권번호
    private String scrNm;                   // 화면명
    private String scrId;                   // 화면ID
    private String lginDttm;                // 사용자가 로그인한 접속일시
    private String lginIpAddr;              // 사용자가 접속한 IP 주소
    private String userNm;                  // 사용자 이름(한글)
    private String userEngNm;               // 사용자 영문이름
    private String userId;                  // 사용자 ID(AD ID)
    private String userNo;                  // 사용자번호
    private String deptCd;                  // 사용자조직 코드
    private String salsDvCd;                // 영업본부코드
    private String salsBoCd;                // 영업지점코드
    private String uppDeptCd;               // 상위조직코드
    private String prcsrUserId;             // 처리자 ID(AD ID)
    private String prcsrUserNo;             // 처리지번호
    private String prcsrDeptCd;             // 처리지조직 코드
    private String prcsrDvCd;               // 처리지 영업본부코드
    private String prcsrBoCd;               // 처리지 영업지점코드
    private String prcsrUppDeptCd;          // 부서코드
    private String sysCd;                   // 요청이 들어온 시스템을 표시
    private String reqtSvcNm;               // 요청하는 서비스 모듈명
    private String reqtMthdNm;              // 요청하는 메소드명
    private String reqtVoNm;                // 요청메소드에 전달할 값을 담는 VO명
    private String scrButnFuncClssCd;       // 화면에서 버튼 별 이벤트 구분을 위한 구분코드
    private String scrGriCnt;               // 화면 그리드 개수
    private List<OlPageDto> pageList;       // 페이징 리스트 (L2 반복)
    private String reqtDttm;                // 요청일시
    private String crdtInfoIcluFlg;         // 신용정보포함여부(Y,N)
    private String crdtInfoDataChgTypCd;    // 업무내역별 식별코드 부여
    private String crdtInfoIdfInEngAbbrNm;  // 신용정보식별영문약어명
    private String crdtInfoIdfnSysCd;       // 신용정보식별시스템코드
    private String scrButnNm;               // 화면버튼명
    private String msgCnt;                  // 메시지 개수
    private List<OlMsgDto> msgList;         // 메시지 리스트 (L2 반복)
    private String respDttm;                // 응답일시
    private String svcRunNm;                // 거래별로 유일한 ServiceExecutionID
    private String stdate;                  // 기준일자

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OlPageDto {
        private String pageSrno;        // 페이지 인덱스값 (L3)
        private String pageInqCnt;      // 한페이지에 조회될 건수 (L3)
        private String nxtButnNm;       // 다음버튼ID (L3)
        private String nxtButnEnbFlg;   // 다음버튼 활성여부 (L3)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OlMsgDto {
        private String msgNo;           // 서버 측에서 세팅한 정상/에러 메시지코드 (L3)
        private String msgTypCd;        // 메시지유형코드 (L3)
        private String msgNm;           // 메시지코드의 내용 (L3)
    }
}
