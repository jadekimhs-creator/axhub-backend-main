package io.shinhanlife.axhub.biz.so.atm.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @package io.shinhanlife.axhub.biz.so.atm.dto
 * @className RoleToolAthrInDto
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleToolAthrInDto {
    private String roleToolAthrId;
    private String systId;
    private String roleNo;
    private String toolId;
    private String puseYn;

    private Date   systRgiDt;
    private String systRgiPrafNo;
    private String systRgiOgnzNo;
    private String systRgiSystCd;
    private String systRgiPrgrId;
    private Date   systChgDt;
    private String systChgPrafNo;
    private String systChgOgnzNo;
    private String systChgSystCd;
    private String systChgPrgrId;
}