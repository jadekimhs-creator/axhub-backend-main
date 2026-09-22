package io.shinhanlife.dat.lib.paging;

import io.shinhanlife.glow.db.dto.PageInfo;
import java.lang.reflect.Method;
import java.util.List;

/** Shared state handling for MCI page-number paging requests. */
public final class PageNumberPagingSupport {

    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_DATA_COUNT = 20;

    public <REQUEST, RESPONSE> MciPage<RESPONSE, PgNumPagingInfo> execute(
            REQUEST request, PgNumPagingInfo pagingInfo, PageNumberPagingAdapter<REQUEST, RESPONSE> adapter) {
        if (request == null) throw new IllegalArgumentException("request is required");
        if (adapter == null) throw new IllegalArgumentException("adapter is required");

        PageInfo requestPageInfo = adapter.getRequestPageInfo(request);
        if (requestPageInfo == null) requestPageInfo = new PageInfo(DEFAULT_PAGE_NO, DEFAULT_PAGE_DATA_COUNT);
        if (requestPageInfo.getPageNo() <= 0) requestPageInfo.setPageNo(DEFAULT_PAGE_NO);
        if (requestPageInfo.getPageDataCc() <= 0) requestPageInfo.setPageDataCc(DEFAULT_PAGE_DATA_COUNT);
        if (pagingInfo != null) {
            if (pagingInfo.getPageNo() > 0) requestPageInfo.setPageNo(pagingInfo.getPageNo());
            if (pagingInfo.getPageDataCc() > 0) requestPageInfo.setPageDataCc(pagingInfo.getPageDataCc());
        }
        adapter.setRequestPageInfo(request, requestPageInfo);

        PageNumberPagingResult<RESPONSE> result = adapter.invoke(request, requestPageInfo);
        if (result == null) throw new IllegalStateException("page-number paging adapter result is required");

        PageInfo responsePageInfo = result.responsePageInfo();
        int currentPageNo = responsePageInfo != null && responsePageInfo.getPageNo() > 0
                ? responsePageInfo.getPageNo() : requestPageInfo.getPageNo();
        int pageDataCc = responsePageInfo != null && responsePageInfo.getPageDataCc() > 0
                ? responsePageInfo.getPageDataCc() : requestPageInfo.getPageDataCc();
        int totalPageCn = responsePageInfo == null ? 0 : responsePageInfo.getTotaPageCn();
        int totalDataCc = responsePageInfo == null ? 0 : responsePageInfo.getTotaPageDataCc();
        boolean hasNext = totalPageCn > 0 ? currentPageNo < totalPageCn
                : totalDataCc > 0 && pageDataCc > 0 && (long) currentPageNo * pageDataCc < totalDataCc;

        PgNumPagingInfo nextPagingInfo = new PgNumPagingInfo(
                currentPageNo + 1, pageDataCc, totalPageCn, totalDataCc, hasNext);
        if (result.response() != null) adapter.setPagingResult(result.response(), nextPagingInfo);
        return new MciPage<>(result.response(), nextPagingInfo);
    }

    public static void setMciPageInfo(Object mciRequest, PageInfo pageInfo) {
        if (mciRequest == null || pageInfo == null) return;
        try {
            mciRequest.getClass().getMethod("setPageInfo", List.class).invoke(mciRequest, List.of(pageInfo));
        } catch (NoSuchMethodException ignored) {
            try {
                mciRequest.getClass().getMethod("setPageInfo", PageInfo.class).invoke(mciRequest, pageInfo);
            } catch (ReflectiveOperationException ignoredAgain) {
                // A contract without pageInfo cannot receive a page-number continuation.
            }
        } catch (ReflectiveOperationException ignored) {
            // Keep compatibility with existing generated MCI DTO contracts.
        }
    }

    public static PageInfo getMciPageInfo(Object mciResponse) {
        if (mciResponse == null) return null;
        try {
            Object value = mciResponse.getClass().getMethod("getPageInfo").invoke(mciResponse);
            if (value instanceof PageInfo pageInfo) return pageInfo;
            if (value instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof PageInfo pageInfo) return pageInfo;
        } catch (ReflectiveOperationException ignored) {
            // A response without pageInfo has no continuation information.
        }
        return null;
    }
}
