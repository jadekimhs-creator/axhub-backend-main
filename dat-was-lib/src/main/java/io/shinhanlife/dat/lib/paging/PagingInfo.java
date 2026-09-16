package io.shinhanlife.dat.lib.paging;

/**
 * @package io.shinhanlife.dat.lib.paging
 * @className PagingInfo
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
public interface PagingInfo<T extends PagingInfo<T>> {

    boolean hasMore();

    T nextPageRequest();
}
