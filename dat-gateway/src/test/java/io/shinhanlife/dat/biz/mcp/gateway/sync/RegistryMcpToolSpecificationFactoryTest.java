package io.shinhanlife.dat.biz.mcp.gateway.sync;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import io.shinhanlife.dat.mcg.sync.RegistryMcpToolSpecificationFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RegistryMcpToolSpecificationFactoryTest {

    @Test
    void exposesMetadataBehaviorHintsInMcpToolSpecification() {
        ToolMetadata metadata = ToolMetadata.builder()
                .name("customer_lookup")
                .displayName("고객 조회")
                .description("Looks up customer information")
                .displayDescription("고객 기본 정보 조회")
                .semver("1.2.0")
                .categoryKey("cus")
                .exampleQueries(List.of("고객 10001을 조회해줘"))
                .tags(List.of("customer", "search"))
                .ownerOrg("CUSTOMER_TEAM")
                .outputSchema(Map.of("type", "object", "properties", Map.of()))
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
        assertEquals("고객 조회", tool.title());
        assertEquals("object", tool.outputSchema().get("type"));
        assertEquals("1.2.0", tool.meta().get("version"));
        assertEquals("CUSTOMER_TEAM", tool.meta().get("owner_org"));
    }
}
