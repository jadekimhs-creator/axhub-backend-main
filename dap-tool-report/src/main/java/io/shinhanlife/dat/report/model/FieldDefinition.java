package io.shinhanlife.dat.report.model;

/** DTO, JSON Schema 또는 원천 전문에서 수집한 필드 정의. */
public record FieldDefinition(
        String toolName,
        String sourceKind,
        String direction,
        String ownerType,
        String fieldName,
        String dataType,
        Boolean required,
        String description,
        String constraints,
        String sourceFile) {
}
