package io.shinhanlife.axhub.sample.converter;

import io.shinhanlife.axhub.sample.domain.model.AppliSystNtfyPatiModel;
import io.shinhanlife.axhub.sample.dto.AppliSystNtfyRgiInDTO;
import io.shinhanlife.axhub.sample.dto.StrnTermListInDTO;
import io.shinhanlife.axhub.sample.dto.StrnTermListOutDTO;
import io.shinhanlife.axhub.sample.presentation.io.AppliSystNtfyPatiRequest;
import io.shinhanlife.axhub.sample.presentation.io.StrnTermRequest;
import io.shinhanlife.axhub.sample.presentation.io.StrnTermResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class SampleConverter {

    public abstract StrnTermListInDTO convertRequestToDto(StrnTermRequest req);

    public abstract StrnTermResponse convertDtoToResponse(StrnTermListOutDTO outDto);

    public abstract AppliSystNtfyPatiModel convertDtoToModel(AppliSystNtfyRgiInDTO inDto);

    public abstract AppliSystNtfyRgiInDTO convertAppliRequestToDto(AppliSystNtfyPatiRequest request);

}
