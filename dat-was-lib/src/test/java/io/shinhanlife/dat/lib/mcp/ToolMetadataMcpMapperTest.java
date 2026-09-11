package io.shinhanlife.dat.lib.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import org.junit.jupiter.api.Test;

class ToolMetadataMcpMapperTest {

    @Test
    void includesUsageGuidanceInMcpMeta() {
        ToolMetadata metadata = new ToolMetadata();
        metadata.setWhenToUse("사용 시점");
        metadata.setWhenNotToUse("사용 제외");
        metadata.setIoLimits("입출력 제한");

        assertThat(ToolMetadataMcpMapper.meta(metadata)).containsEntry("when_to_use", "사용 시점")
                .containsEntry("when_not_to_use", "사용 제외")
                .containsEntry("io_limits", "입출력 제한");
    }
}
