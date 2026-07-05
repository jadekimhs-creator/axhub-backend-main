package io.shinhanlife.axhub.common.session.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ZtUsacOutDto {

    /* 인사번호 */
    private String prafNo;
    /* 인사명 */
    private String prafNm;
    /* 조직번호 */
    private String ognzNo;
    /* 조직명 */
    private String ognzNm;
    /* 이메일주소 */
//    @GlowSecureField(type = DataSecureType.DECRYPT_DB, direction = SafeDBType.COMM) TODO 방화벽 뚫리면 확인
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
    /* 사용여부 */
    private String puseYn;

    private String roleNoStrList;
    private String roleNmStrList;
    private String tgtrPrafNoStrList;
    private String tgtrOgnzNoStrList;

    private List<String> roleNoList;
    private List<String> roleNmList;
    private List<String> tgtrPrafNoList;
    private List<String> tgtrOgnzNoList;

    public void initLists() {
        this.roleNoList = convertStrToList(roleNoStrList);
        this.roleNmList = convertStrToList(roleNmStrList);
        this.tgtrPrafNoList = convertStrToList(tgtrPrafNoStrList);
        this.tgtrOgnzNoList = convertStrToList(tgtrOgnzNoStrList);
    }

    private List<String> convertStrToList(String str) {
        if (str == null || str.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(str.split(","));
    }

}
