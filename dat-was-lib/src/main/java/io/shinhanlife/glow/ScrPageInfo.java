package io.shinhanlife.glow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;

/**
 * @package io.shinhanlife.glow
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
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class ScrPageInfo extends io.shinhanlife.glow.db.dto.ScrPageInfo {

    public ScrPageInfo() {
        super();
    }

    public ScrPageInfo(String scrImhdNm, String scrItva, String scrSortValu, String nextDataExtYn, int pageDataCc) {
        super(scrImhdNm, scrItva, scrSortValu, nextDataExtYn, pageDataCc);
    }
}
