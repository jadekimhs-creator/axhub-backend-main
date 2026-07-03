package io.shinhanlife.axhub.biz.sm.mmg.usecase;


import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmMmg0000M01RRequest;
import io.shinhanlife.axhub.biz.sm.mmg.presentation.io.SmMmg0000M01RResponse;

import java.util.List;

public interface SmMmg0000MUseCase {

    public List<SmMmg0000M01RResponse> selectMenu(SmMmg0000M01RRequest req);
}
