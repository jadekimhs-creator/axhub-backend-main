package io.shinhanlife.glow.db.dto;

import java.util.Date;

public class AuditInfo {
    private Date systRgiDt;         // 시스템등록일시
    private String systRgiPrafNo;   // 시스템등록인사번호
    private String systRgiOgnzNo;   // 시스템등록조직번호
    private String systRgiSystCd;   // 시스템등록시스템코드
    private String systRgiPrgrId;   // 시스템등록프로그램ID
    private Date systChgDt;         // 시스템변경일시
    private String systChgPrafNo;   // 시스템변경인사번호
    private String systChgOgnzNo;   // 시스템변경조직번호
    private String systChgSystCd;   // 시스템변경시스템코드
    private String systChgPrgrId;   // 시스템변경프로그램ID
}
