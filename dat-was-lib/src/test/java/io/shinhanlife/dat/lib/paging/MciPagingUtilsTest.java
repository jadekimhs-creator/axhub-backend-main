package io.shinhanlife.dat.lib.paging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.shinhanlife.glow.db.dto.PageInfo;
import io.shinhanlife.glow.db.dto.ScrPageInfo;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MciPagingUtilsTest {

    @Test
    void replacesPageNumberAndScrollPagingMetadataWhileAppendingBusinessLists() {
        PagingResponse accumulated = new PagingResponse(
                new ArrayList<>(List.of("first")),
                new ArrayList<>(List.of(new PageInfo(1, 20))),
                new ScrPageInfo());
        ScrPageInfo lastScrollPageInfo = new ScrPageInfo();
        PagingResponse nextPage = new PagingResponse(
                List.of("second"),
                List.of(new PageInfo(2, 20)),
                lastScrollPageInfo);

        MciPagingUtils.mergeResponseData(accumulated, nextPage);

        assertEquals(List.of("first", "second"), accumulated.items);
        assertEquals(1, accumulated.pageInfo.size());
        assertEquals(2, accumulated.pageInfo.getFirst().getPageNo());
        assertSame(lastScrollPageInfo, accumulated.scrPageInfo);
    }

    private static final class PagingResponse {
        private List<String> items;
        private List<PageInfo> pageInfo;
        private ScrPageInfo scrPageInfo;

        private PagingResponse(List<String> items, List<PageInfo> pageInfo, ScrPageInfo scrPageInfo) {
            this.items = items;
            this.pageInfo = pageInfo;
            this.scrPageInfo = scrPageInfo;
        }
    }
}
