package io.shinhanlife.dat.lib.paging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.shinhanlife.glow.db.dto.PageInfo;
import org.junit.jupiter.api.Test;

class PageNumberPagingSupportTest {

    @Test
    void usesPageDataCountSuppliedByThePod() {
        SampleRequest request = new SampleRequest();

        new PageNumberPagingSupport().execute(request, null, 7, new PageNumberPagingAdapter<SampleRequest, String>() {
            @Override
            public PageInfo getRequestPageInfo(SampleRequest source) {
                return source.pageInfo;
            }

            @Override
            public void setRequestPageInfo(SampleRequest source, PageInfo pageInfo) {
                source.pageInfo = pageInfo;
            }

            @Override
            public PageNumberPagingResult<String> invoke(SampleRequest source, PageInfo pageInfo) {
                return new PageNumberPagingResult<>("ok", null);
            }

            @Override
            public void setPagingResult(String response, PgNumPagingInfo pagingInfo) {
            }
        });

        assertEquals(7, request.pageInfo.getPageDataCc());
    }

    private static final class SampleRequest {
        private PageInfo pageInfo;
    }
}
