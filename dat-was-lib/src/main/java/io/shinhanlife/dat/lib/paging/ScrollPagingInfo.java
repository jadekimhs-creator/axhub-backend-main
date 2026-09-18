package io.shinhanlife.dat.lib.paging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @package io.shinhanlife.dat.lib.paging
 * @className ScrollPagingInfo
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
public class ScrollPagingInfo implements PagingInfo<ScrollPagingInfo>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 스크롤항목명 (입력값)
     */
    private String scrImhdNm;

    /**
     * 스크롤항목값 (입력값)
     */
    private String scrItva;

    /**
     * 스크롤정렬값 (리턴/입력값)
     */
    private String scrSortValu;

    /**
     * 다음데이터존재여부 (리턴값, Y/N)
     */
    private String nextDataExtYn;

    /**
     * 페이지데이터건수 (열 건수, 입력값)
     */
    private int pageDataCc;

    public ScrollPagingInfo(String scrImhdNm, String scrItva, String scrSortValu, boolean hasNext, int pageDataCc) {
        this.scrImhdNm = scrImhdNm;
        this.scrItva = scrItva;
        this.scrSortValu = scrSortValu;
        this.nextDataExtYn = hasNext ? "Y" : "N";
        this.pageDataCc = pageDataCc;
    }

    public static ScrollPagingInfo firstPage(String scrImhdNm, int pageDataCnt) {
        return new ScrollPagingInfo(scrImhdNm, "", "", "N", pageDataCnt);
    }

    public String scrImhdNm() {
        return scrImhdNm;
    }

    public String scrItva() {
        return scrItva;
    }

    public String scrSortValu() {
        return scrSortValu;
    }

    public String nextDataExtYn() {
        return nextDataExtYn;
    }

    public int pageDataCnt() {
        return pageDataCc;
    }

    public boolean hasNext() {
        return hasNextData();
    }

    public boolean hasNextData() {
        return "Y".equalsIgnoreCase(nextDataExtYn);
    }

    @Override
    public boolean hasMore() {
        return hasNextData();
    }

    @Override
    public ScrollPagingInfo nextPageRequest() {
        return this;
    }


}
