package io.shinhanlife.glow.db.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.shinhanlife.glow.GlowTrgmField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @package io.shinhanlife.glow.db.dto
 * @className ScrPageInfo
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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScrPageInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 스크롤항목명 (입력값)
     */
    @Schema(description = "스크롤항목명", example = "inonNo")
    @GlowTrgmField(order = 1, length = 100, description = "스크롤항목명")
    private String scrImhdNm;

    /**
     * 스크롤항목값 (입력값)
     */
    @Schema(description = "스크롤항목값", example = "10")
    @GlowTrgmField(order = 2, length = 100, description = "스크롤항목값")
    private String scrItva;

    /**
     * 스크롤정렬값 (리턴/입력값)
     */
    @Schema(description = "스크롤정렬값", example = "202609010001")
    @GlowTrgmField(order = 3, length = 100, description = "스크롤정렬값")
    private String scrSortValu;

    /**
     * 다음데이터존재여부 (리턴값, Y/N)
     */
    @Schema(description = "다음데이터존재여부", example = "Y")
    @GlowTrgmField(order = 4, length = 1, description = "다음데이터존재여부")
    private String nextDataExtYn;

    /**
     * 페이지데이터건수 (입력값)
     */
    @Schema(description = "페이지데이터건수", example = "20")
    @GlowTrgmField(order = 5, length = 5, description = "페이지데이터건수")
    private int pageDataCc;
}
