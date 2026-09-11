package io.shinhanlife.dat.lib.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** BC-DAB-STD-003 V17 Tool 정의 파일의 불변 모델입니다. */
public record ToolDefinition(
        String name,
        @JsonProperty("display_name") String displayName,
        String version,
        @JsonProperty("category_key") String categoryKey,
        ToolDescription description,
        @JsonProperty("display_description") String displayDescription,
        @JsonProperty("example_queries") List<String> exampleQueries,
        @JsonProperty("read_only") Boolean readOnly,
        Boolean destructive,
        Boolean idempotent,
        @JsonProperty("parameters_schema") Map<String, Object> parametersSchema,
        @JsonProperty("output_schema") Map<String, Object> outputSchema,
        List<String> tags,
        @JsonProperty("legacy_interface_id") String legacyInterfaceId,
        @JsonProperty("required_env_keys") List<String> requiredEnvKeys,
        @JsonProperty("owner_org") String ownerOrg) {

    public ToolDefinition withExampleQueries(List<String> queries) {
        return new ToolDefinition(name, displayName, version, categoryKey, description, displayDescription,
                queries, readOnly, destructive, idempotent, parametersSchema, outputSchema, tags, legacyInterfaceId,
                requiredEnvKeys, ownerOrg);
    }

    public ToolDefinition withName(String value) {
        return new ToolDefinition(value, displayName, version, categoryKey, description, displayDescription,
                exampleQueries, readOnly, destructive, idempotent, parametersSchema, outputSchema, tags, legacyInterfaceId,
                requiredEnvKeys, ownerOrg);
    }
}
