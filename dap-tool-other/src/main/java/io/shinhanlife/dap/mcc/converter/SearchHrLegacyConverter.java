package io.shinhanlife.dap.mcc.converter;

import io.shinhanlife.dap.mcc.dto.SearchHrReq;
import io.shinhanlife.dap.mcc.dto.SearchHrRes;
import io.shinhanlife.dap.mcc.legacy.SearchHrLegacyReq;
import io.shinhanlife.dap.mcc.legacy.SearchHrLegacyRes;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @package io.shinhanlife.dap.mcc.converter
 * @className SearchHrLegacyConverter
 * @description AX HUB 시스템 처리 클래스
 * @author user
 * @create 2026.07.22
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.07.22  user    최초생성
 *
 * </pre>
 */
@Mapper(componentModel = "spring")
public interface SearchHrLegacyConverter {

    @Mapping(source = "phoneNumber", target = "phone")
    @Mapping(source = "message", target = "content")
    SearchHrLegacyReq toLegacyReq(SearchHrReq req);

    @Mapping(source = "phone", target = "phoneNumber")
    @Mapping(source = "content", target = "message")
    SearchHrReq toReq(SearchHrLegacyReq legacyReq);

    // SearchHrRes toRes(SearchHrLegacyRes legacyRes);
}
