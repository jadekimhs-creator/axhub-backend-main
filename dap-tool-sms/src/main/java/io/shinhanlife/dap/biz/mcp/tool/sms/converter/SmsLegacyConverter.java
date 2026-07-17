package io.shinhanlife.dap.biz.mcp.tool.sms.converter;


/**
 * @package io.shinhanlife.dap.biz.mcp.tool.sms.converter
 * @className SmsLegacyConverter
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
import io.shinhanlife.dap.biz.mcp.tool.dto.SmsSendReq;
import io.shinhanlife.dap.biz.mcp.tool.sms.dto.SmsLegacyReqDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SmsLegacyConverter {

    @Mapping(source = "phoneNumber", target = "phone")
    @Mapping(source = "message", target = "content")
    SmsLegacyReqDto toLegacyReq(SmsSendReq req);
}
