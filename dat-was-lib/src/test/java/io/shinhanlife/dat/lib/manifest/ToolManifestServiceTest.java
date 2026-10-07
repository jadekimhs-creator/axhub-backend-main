package io.shinhanlife.dat.lib.manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dat.lib.config.McpProperties;
import io.shinhanlife.dat.lib.config.ToolServiceProperties;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolManifestServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildsStandardManifestAndDerivesRevisionFromToolDefinition() {
        McpProperties properties = manifestProperties("insurance-processing", "processing.");
        ToolManifestService service = new ToolManifestService(
                () -> List.of(tool("processing.contract.inquiry", "1.2.0", 3000)), objectMapper, properties);

        ToolManifestResponse manifest = service.currentManifest();

        assertEquals("insurance-processing", manifest.bundleId());
        assertTrue(manifest.revision().matches("\\d+"));
        assertEquals(1, manifest.tools().size());
        ToolManifestItem item = manifest.tools().getFirst();
        assertEquals("processing.contract.inquiry", item.name());
        assertEquals("http://tool-processing.ax-hub.svc.cluster.local:8080/mcp/processing.contract.inquiry",
                item.endpoint());
        assertEquals("계약 조회", item.title());
        assertEquals("object", item.inputSchema().get("type"));
        assertTrue(item.annotations().readOnlyHint());
        assertEquals("1.2.0", item.meta().version());
        assertEquals(3000, item.meta().timeoutMillis());
        assertEquals(5, item.meta().retryMaxAttempts());
        assertEquals(List.of("계약 상태를 알려줘", "내 계약을 조회해줘", "계약번호로 찾아줘"),
                item.meta().exampleQueries());
    }

    @Test
    void changesRevisionWhenToolDefinitionChanges() {
        McpProperties properties = manifestProperties("insurance-processing", "processing.");
        ToolManifestService before = new ToolManifestService(
                () -> List.of(tool("processing.contract.inquiry", "1.2.0", 3000)), objectMapper, properties);
        ToolManifestService after = new ToolManifestService(
                () -> List.of(tool("processing.contract.inquiry", "1.2.0", 5000)), objectMapper, properties);

        assertTrue(!before.currentManifest().revision().equals(after.currentManifest().revision()));
    }

    @Test
    void serializesToolServiceManifestWithServiceIdInsteadOfBundleId() throws Exception {
        McpProperties properties = manifestProperties("was-cus", "cus.");
        ToolManifestService service = new ToolManifestService(() -> List.of(), objectMapper, properties);

        String json = objectMapper.writeValueAsString(service.currentToolServiceManifest());

        assertTrue(json.contains("\"service_id\":\"was-cus\"") || json.contains("\"serviceId\":\"was-cus\""));
        assertTrue(!json.contains("\"bundleId\""));
    }

    @Test
    @SuppressWarnings("unchecked")
    void serializesRoutingContractWithNewSchemaFieldsAndListValues() throws Exception {
        McpProperties properties = new McpProperties();
        McpProperties.Manifest manifest = new McpProperties.Manifest();
        manifest.setServiceId("contract-service");

        McpProperties.RoutingFunction contract = new McpProperties.RoutingFunction();
        contract.setBusinessDomain(List.of("계약", "보장", "해지"));
        contract.setBusinessOutcome(List.of("계약 기본정보", "보장 내용"));
        contract.setPrimaryEntities(List.of("계약", "증권", "담보"));
        contract.setSelectIf(List.of("계약·보장 내용을 확인하는 요청"));
        contract.setRejectIf(List.of("보험금 청구", "민원 처리"));
        contract.setCapabilityIndex(List.of("policy_contract_lookup", "policy_coverage_lookup"));
        contract.setConfusableServers(List.of("claim-service", "customer-service"));
        manifest.setRoutingContract(contract);
        properties.setManifest(manifest);

        ToolManifestService service = new ToolManifestService(() -> List.of(), objectMapper, properties);
        ToolServiceManifestResponse response = service.currentToolServiceManifest();

        assertEquals("contract-service", response.serviceId());
        assertNotNull(response.routingContract());

        Map<String, Object> rc = response.routingContract();
        assertEquals(List.of("계약", "보장", "해지"), rc.get("business_domain"));
        assertEquals(List.of("계약 기본정보", "보장 내용"), rc.get("business_outcome"));
        assertEquals(List.of("계약", "증권", "담보"), rc.get("primary_entities"));
        assertEquals(List.of("계약·보장 내용을 확인하는 요청"), rc.get("select_if"));
        assertEquals(List.of("보험금 청구", "민원 처리"), rc.get("reject_if"));
        assertEquals(List.of("policy_contract_lookup", "policy_coverage_lookup"), rc.get("capability_index"));
        assertEquals(List.of("claim-service", "customer-service"), rc.get("confusable_servers"));

        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"routing_contract\""));
        assertTrue(json.contains("\"business_domain\":[\"계약\",\"보장\",\"해지\"]"));
        assertTrue(json.contains("\"capability_index\":[\"policy_contract_lookup\",\"policy_coverage_lookup\"]"));
        assertTrue(!json.contains("\"routing_functions\""));
        assertTrue(!json.contains("\"routingFunctions\""));
        assertTrue(!json.contains("\"serviceId\""));
    }

    @Test
    void rejectsEntireManifestWhenToolNameDoesNotMatchConfiguredPrefix() {
        McpProperties properties = manifestProperties("insurance-processing", "processing.");
        ToolManifestService service = new ToolManifestService(
                () -> List.of(tool("notification_sms_send", "1.0.0", 3000)), objectMapper, properties);

        IllegalStateException error = assertThrows(IllegalStateException.class, service::currentManifest);

        assertTrue(error.getMessage().contains("name-prefix"));
    }

    @Test
    void rejectsEntireManifestWhenToolNamesAreDuplicated() {
        McpProperties properties = manifestProperties("insurance-processing", "processing.");
        ToolManifestService service = new ToolManifestService(
                () -> List.of(tool("processing.contract.inquiry", "1.0.0", 3000),
                        tool("processing.contract.inquiry", "1.0.1", 3000)), objectMapper, properties);

        IllegalStateException error = assertThrows(IllegalStateException.class, service::currentManifest);

        assertTrue(error.getMessage().contains("duplicate"));
    }

    private McpProperties manifestProperties(String bundleId, String namePrefix) {
        McpProperties properties = new McpProperties();
        McpProperties.Manifest manifest = new McpProperties.Manifest();
        manifest.setBundleId(bundleId);
        manifest.setNamePrefix(namePrefix);
        properties.setManifest(manifest);
        return properties;
    }

    private ToolMetadata tool(String name, String version, long timeoutMillis) {
        return ToolMetadata.builder()
                .name(name)
                .podUrl("http://tool-processing.ax-hub.svc.cluster.local:8080")
                .displayName("계약 조회")
                .description("계약번호로 계약 정보를 조회합니다.")
                .exampleQueries(List.of("계약 상태를 알려줘", "내 계약을 조회해줘", "계약번호로 찾아줘"))
                .tags(List.of("계약"))
                .ownerOrg("MCP_TOOL")
                .parametersSchema(Map.of("type", "object", "properties", Map.of("contractNo", Map.of("type", "string")),
                        "required", List.of("contractNo"), "additionalProperties", false))
                .semver(version)
                .timeoutMillis(timeoutMillis)
                .retryMaxAttempts(5)
                .enabled(true)
                .readOnlyHint(true)
                .destructiveHint(false)
                .idempotentHint(true)
                .openWorldHint(false)
                .build();
    }
}
