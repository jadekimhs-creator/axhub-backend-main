package io.shinhanlife.dap.biz.mcp.gateway.sync;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.shinhanlife.dap.biz.mcp.gateway.dto.ToolMetadata;
import org.junit.jupiter.api.Test;

class RegistryMcpToolSpecificationFactoryTest {

    @Test
    void exposesMetadataBehaviorHintsInMcpToolSpecification() {
        ToolMetadata metadata = ToolMetadata.builder()
                .name("customer_lookup")
                .description("Looks up customer information")
                .readOnlyHint(true)
                .destructiveHint(false)
                .idempotentHint(true)
                .openWorldHint(false)
                .build();

        RegistryMcpToolSpecificationFactory factory = new RegistryMcpToolSpecificationFactory(null, new ObjectMapper());
        McpSchema.Tool tool = factory.create(metadata).tool();

        assertTrue(tool.annotations().readOnlyHint());
        assertFalse(tool.annotations().destructiveHint());
        assertTrue(tool.annotations().idempotentHint());
        assertFalse(tool.annotations().openWorldHint());
    }
}
