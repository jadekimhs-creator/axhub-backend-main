package io.shinhanlife.axhub.sample.usecase;

import io.shinhanlife.axhub.sample.presentation.io.AppliSystNtfyPatiRequest;
import io.shinhanlife.axhub.sample.presentation.io.AppliSystNtfyPatiResponse;
import io.shinhanlife.axhub.sample.presentation.io.StrnTermRequest;
import io.shinhanlife.axhub.sample.presentation.io.StrnTermResponse;

public interface GlowSampleUseCase {
    StrnTermResponse getStrnTerms(StrnTermRequest req);
    AppliSystNtfyPatiResponse insertAppliSystNtfyPati(AppliSystNtfyPatiRequest req);
}
