package io.shinhanlife.dat.lib.paging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @package io.shinhanlife.dat.lib.paging
 * @className MciPage
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
public class MciPage<T, P> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private T data;
    private List<T> items;
    private P pagingInfo;

    @SuppressWarnings("unchecked")
    public MciPage(T data, P pagingInfo) {
        this.data = data;
        this.pagingInfo = pagingInfo;
        if (data instanceof List<?> list) {
            this.items = (List<T>) list;
        }
    }

    @SuppressWarnings("unchecked")
    public MciPage(List<T> items, P pagingInfo) {
        this.items = items;
        this.pagingInfo = pagingInfo;
        this.data = (T) items;
    }

    public T data() {
        return data;
    }

    public P pagingInfo() {
        return pagingInfo;
    }

    @SuppressWarnings("unchecked")
    public List<T> items() {
        if (items != null) {
            return items;
        }
        if (data instanceof List<?> list) {
            return (List<T>) list;
        }
        return data != null ? List.of(data) : List.of();
    }

    public boolean hasNext() {
        if (pagingInfo instanceof ScrollPagingInfo spi) {
            return spi.hasNext();
        }
        if (pagingInfo instanceof PgNumPagingInfo ppi) {
            return ppi.hasNext();
        }
        if (pagingInfo instanceof PagingInfo<?> pi) {
            return pi.hasMore();
        }
        return false;
    }

    public boolean hasMore() {
        return hasNext();
    }
}
