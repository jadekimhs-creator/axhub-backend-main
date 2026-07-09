package io.shinhanlife.axhub.sample.converter;

import io.shinhanlife.axhub.sample.domain.model.AppliSystNtfyPatiModel;
import io.shinhanlife.axhub.sample.dto.AppliSystNtfyRgiInDTO;
import io.shinhanlife.axhub.sample.dto.StrnTermListInDTO;
import io.shinhanlife.axhub.sample.dto.StrnTermListOutDTO;
import io.shinhanlife.axhub.sample.presentation.io.AppliSystNtfyPatiRequest;
import io.shinhanlife.axhub.sample.presentation.io.StrnTermRequest;
import io.shinhanlife.axhub.sample.presentation.io.StrnTermResponse;
import org.mapstruct.Mapper;

/**
 * @package io.shinhanlife.axhub.sample.converter
 * @className SampleConverter
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
@Mapper(componentModel = "spring")
public abstract class SampleConverter {

    public abstract StrnTermListInDTO convertRequestToDto(StrnTermRequest req);

    public abstract StrnTermResponse convertDtoToResponse(StrnTermListOutDTO outDto);

    public abstract AppliSystNtfyPatiModel convertDtoToModel(AppliSystNtfyRgiInDTO inDto);

    public abstract AppliSystNtfyRgiInDTO convertAppliRequestToDto(AppliSystNtfyPatiRequest request);

}