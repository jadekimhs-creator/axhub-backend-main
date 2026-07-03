package io.shinhanlife.axhub.biz.sm.mmg.usecase;

import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01DRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01RRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01RResponse;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01SRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmNmg0100M01SResponse;

import java.util.List;

public interface SmNmg0100MUseCase {

    List<SmNmg0100M01RResponse> selectMenu(SmNmg0100M01RRequest req);

    SmNmg0100M01SResponse saveMenu(SmNmg0100M01SRequest req);

    void deleteMenu(SmNmg0100M01DRequest req);
}