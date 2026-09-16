package io.shinhanlife.dat.lib.paging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @package io.shinhanlife.dat.lib.paging
 * @className PgNumPagingInfo
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
public class PgNumPagingInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 페이지번호 (입력값)
     */
    private int pageNo;

    /**
     * 페이지 데이터 건수 (열 건수, 입력값)
     */
    private int pageDataCc;

    /**
     * 총페이지 수 (리턴값)
     */
    private int totaPageCn;

    /**
     * 총 페이지 데이터 건수 (리턴값)
     */
    private int totaPageDataCc;

    /**
     * 다음 페이지 존재 여부
     */
    private boolean hasNext;

    public PgNumPagingInfo(int pageNo, int pageDataCc, int totaPageCn, int totaPageDataCc) {
        this.pageNo = pageNo;
        this.pageDataCc = pageDataCc;
        this.totaPageCn = totaPageCn;
        this.totaPageDataCc = totaPageDataCc;
        this.hasNext = totaPageCn > 0 ? pageNo <= totaPageCn : (totaPageDataCc > 0 && pageDataCc > 0 && (long) (pageNo - 1) * pageDataCc < totaPageDataCc);
    }

    public boolean hasNext() {
        if (hasNext) {
            return true;
        }
        if (totaPageCn > 0) {
            return pageNo <= totaPageCn;
        }
        if (totaPageDataCc > 0 && pageDataCc > 0) {
            return (long) (pageNo - 1) * pageDataCc < totaPageDataCc;
        }
        return false;
    }
}