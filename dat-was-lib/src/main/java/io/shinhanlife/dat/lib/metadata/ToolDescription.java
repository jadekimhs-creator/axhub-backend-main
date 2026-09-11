package io.shinhanlife.dat.lib.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

/** LLM이 Tool 선택 여부를 판단할 때 사용하는 V17 목적 설명입니다. */
public record ToolDescription(
        String function,
        @JsonProperty("when_to_use") String whenToUse,
        @JsonProperty("when_not_to_use") String whenNotToUse,
        @JsonProperty("io_limits") String ioLimits) {
}
