package io.shinhanlife.dat.report.model;

/** UI 선택 목록과 보고서 기본정보에 사용하는 툴 요약. */
public record ToolSummary(
        String name,
        String title,
        String description,
        String categoryKey,
        String mappingId,
        boolean register,
        boolean requiresApproval,
        boolean readOnlyHint,
        boolean destructiveHint,
        boolean idempotentHint,
        boolean openWorldHint,
        String requestType,
        String responseType,
        String useCaseClass,
        String sourceFile,
        String inputSchemaResource,
        String outputSchemaResource,
        String version,
        String functionDescription,
        String whenToUse,
        String whenNotToUse,
        String ioLimits,
        String displayDescription,
        String exampleQueries,
        String tags,
        String requiredEnvKeys,
        String ownerOrg,
        String definitionFile) {

    public ToolSummary(String name, String title, String description, String categoryKey, String mappingId,
                       boolean register, boolean requiresApproval, boolean readOnlyHint, boolean destructiveHint,
                       boolean idempotentHint, boolean openWorldHint, String requestType, String responseType,
                       String useCaseClass, String sourceFile, String inputSchemaResource, String outputSchemaResource) {
        this(name, title, description, categoryKey, mappingId, register, requiresApproval, readOnlyHint,
                destructiveHint, idempotentHint, openWorldHint, requestType, responseType, useCaseClass, sourceFile,
                inputSchemaResource, outputSchemaResource, "", "", "", "", "", "", "", "", "", "", "");
    }
}
