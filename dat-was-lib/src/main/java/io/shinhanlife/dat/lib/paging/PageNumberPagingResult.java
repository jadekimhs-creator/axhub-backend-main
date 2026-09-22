package io.shinhanlife.dat.lib.paging;

import io.shinhanlife.glow.db.dto.PageInfo;

/** Result returned from a generated Pod's page-number MCI invocation. */
public record PageNumberPagingResult<RESPONSE>(RESPONSE response, PageInfo responsePageInfo) {
}
