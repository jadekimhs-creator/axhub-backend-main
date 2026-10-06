package io.shinhanlife.dat.lib.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.modelcontextprotocol.spec.McpSchema;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolMetadataMcpMapperTest {

    @Test
    void exposesResilienceSettingsInMcpToolMeta() {
        ToolMetadata metadata = ToolMetadata.builder()
                .timeoutMillis(12000L)
                .retryMaxAttempts(5)
                .build();

        Map<String, Object> meta = ToolMetadataMcpMapper.meta(metadata);

        assertEquals(12000L, meta.get("timeoutMillis"));
        assertFalse(meta.containsKey("retryEnabled"));
        assertEquals(5, meta.get("retryMaxAttempts"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void unwrapsNestedDtoPropertiesAndPromotesRequiredToTopLevel() {
        // Given: inputSchema > properties > DTO명 > properties / required 구조
        Map<String, Object> nestedSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "CmmClaimSearchRequest", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "claimNo", Map.of("type", "string", "description", "보험금 청구번호"),
                                        "customerName", Map.of("type", "string", "description", "고객 성명")
                                ),
                                "required", List.of("claimNo")
                        )
                )
        );

        ToolMetadata metadata = ToolMetadata.builder()
                .name("cmm_claim_search")
                .parametersSchema(nestedSchema)
                .build();

        // When: MCP Tool 명세로 변환
        McpSchema.Tool tool = ToolMetadataMcpMapper.toTool(metadata);

        // Then: 최상위 properties에 claimNo, customerName이 직접 위치하고, required가 최상위에 위치
        assertNotNull(tool.inputSchema());
        assertEquals("object", tool.inputSchema().get("type"));

        Map<String, Object> properties = (Map<String, Object>) tool.inputSchema().get("properties");
        assertNotNull(properties);
        assertTrue(properties.containsKey("claimNo"));
        assertTrue(properties.containsKey("customerName"));
        assertFalse(properties.containsKey("CmmClaimSearchRequest"));

        List<String> required = (List<String>) tool.inputSchema().get("required");
        assertNotNull(required);
        assertEquals(List.of("claimNo"), required);
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsAlreadyFlatSchemaIntact() {
        // Given: 이미 평탄화된 정상 표준 스키마
        Map<String, Object> flatSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "claimNo", Map.of("type", "string"),
                        "customerName", Map.of("type", "string")
                ),
                "required", List.of("claimNo")
        );

        ToolMetadata metadata = ToolMetadata.builder()
                .name("cmm_claim_search")
                .parametersSchema(flatSchema)
                .build();

        McpSchema.Tool tool = ToolMetadataMcpMapper.toTool(metadata);

        Map<String, Object> properties = (Map<String, Object>) tool.inputSchema().get("properties");
        assertNotNull(properties);
        assertTrue(properties.containsKey("claimNo"));
        assertTrue(properties.containsKey("customerName"));
        assertEquals(List.of("claimNo"), tool.inputSchema().get("required"));
    }
}
