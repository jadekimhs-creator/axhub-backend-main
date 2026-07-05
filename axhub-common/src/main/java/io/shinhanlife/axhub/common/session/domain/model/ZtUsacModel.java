package io.shinhanlife.axhub.common.session.domain.model;

import io.shinhanlife.glow.db.dto.AuditInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
