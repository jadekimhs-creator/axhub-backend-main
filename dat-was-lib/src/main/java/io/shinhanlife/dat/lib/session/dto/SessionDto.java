package io.shinhanlife.dat.lib.session.dto;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @package io.shinhanlife.dat.lib.session.dto
 * @className SessionDto
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
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionDto {

    /* 인사번호 */
    private String prafNo;
    /* 인사명 */
    private String prafNm;
    /* 조직번호 */
    private String ognzNo;
    /* 조직번호 */
    private String ognzNm;
    /* 이메일주소 */
    private String addre;
    /* 인사직무코드 */
    private String prafOfduCd;
    /* 인사직무명 */
    private String prafOfduNm;
    /* 인사직급코드 */
    private String prafOfleCd;
    /* 인사직급명 */
    private String prafOfleNm;
    /* 인사직책코드 */
    private String prafDutyCd;
    /* 인사직책명 */
    private String prafDutyNm;

    private List<String> roleNoList;
    private List<String> roleNmList;
    private List<String> tgtrPrafNoList;
    private List<String> tgtrOgnzNoList;
    
    // 추가된 LICO 연동 공통 헤더 필수 필드들
    private String strYmd;
    private String brafNo;
    private String psmrAsrtCd;
    private String sbsnRulpAsrtCd;
    private String bsduCd;
    private String bsquCd;
    private String ognzAsrtCd;
    private String ognzLeveCd;
    private String prgrId;


    // 유틸성
    private String loginDtm; // 로그인일시
    private String isManager; // 관리자여부

    public void setLoginDtm() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        this.loginDtm = LocalDateTime.now().format(formatter);
    }

    public void setIsManager(String isManager) {
        // TODO 역할 필터링 후 관리자 여부 체크
        this.isManager = "Y";
    }



}