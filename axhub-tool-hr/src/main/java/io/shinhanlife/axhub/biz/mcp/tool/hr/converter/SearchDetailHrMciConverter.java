package io.shinhanlife.axhub.biz.mcp.tool.hr.converter;

import io.shinhanlife.axhub.biz.mcp.tool.dto.SearchDetailHrReq;
import io.shinhanlife.axhub.biz.mcp.tool.dto.SearchDetailHrRes;
import io.shinhanlife.axhub.biz.mcp.tool.hr.dto.SearchDetailHrMciReqDto;
import io.shinhanlife.axhub.biz.mcp.tool.hr.dto.SearchDetailHrMciResDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @package io.shinhanlife.axhub.biz.mcp.tool.hr.converter
 * @className SearchDetailHrMciConverter
 * @description AX HUB 시스템 처리 클래스
 * @author root
 * @create 2026.07.13
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.07.13  root    최초생성
 *
 * </pre>
 */
@Mapper(componentModel = "spring")
public interface SearchDetailHrMciConverter {

    SearchDetailHrMciConverter INSTANCE = Mappers.getMapper(SearchDetailHrMciConverter.class);

    // @Mapping(source = "sourceField", target = "targetField")
    SearchDetailHrMciReqDto toMciReq(SearchDetailHrReq req);

    SearchDetailHrRes toRes(SearchDetailHrMciResDto mciRes);
}
