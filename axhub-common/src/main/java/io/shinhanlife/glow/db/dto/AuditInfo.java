package io.shinhanlife.glow.db.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

/**
 * @package io.shinhanlife.glow.db.dto
 * @className AuditInfo
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
@Setter
@NoArgsConstructor
@AllArgsConstructor
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