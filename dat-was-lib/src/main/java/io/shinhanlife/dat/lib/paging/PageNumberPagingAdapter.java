package io.shinhanlife.dat.lib.paging;

import io.shinhanlife.glow.db.dto.PageInfo;

/** Boundary between a generated Pod and the common page-number paging flow. */
public interface PageNumberPagingAdapter<REQUEST, RESPONSE> {

    PageInfo getRequestPageInfo(REQUEST request);

    void setRequestPageInfo(REQUEST request, PageInfo pageInfo);

    PageNumberPagingResult<RESPONSE> invoke(REQUEST request, PageInfo pageInfo);

    void setPagingResult(RESPONSE response, PgNumPagingInfo pagingInfo);
}
