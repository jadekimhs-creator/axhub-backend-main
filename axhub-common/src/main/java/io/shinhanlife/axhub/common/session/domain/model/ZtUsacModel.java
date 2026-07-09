package io.shinhanlife.axhub.common.session.domain.model;

import io.shinhanlife.glow.db.dto.AuditInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @package io.shinhanlife.axhub.common.session.domain.model
 * @className ZtUsacModel
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
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ZtUsacModel extends AuditInfo {

    /* 인사번호 */
    private String prafNo;
    /* 인사명 */
    private String prafNm;
    /* 조직번호 */
    private String ognzNo;
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

}