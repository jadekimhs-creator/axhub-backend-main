package io.shinhanlife.dat.mcg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import org.junit.jupiter.api.Test;

class ExecuteServiceRetryPolicyTest {

    @Test
    void toolRetryMaxAttemptsOverridesGatewayDefault() {
        ToolMetadata metadata = ToolMetadata.builder().retryMaxAttempts(5).build();

        assertEquals(5, ExecuteService.retryMaxAttempts(metadata, 3));
        metadata.setRetryMaxAttempts(0);
        assertEquals(3, ExecuteService.retryMaxAttempts(metadata, 3));
    }
}
