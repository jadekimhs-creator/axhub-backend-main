package io.shinhanlife.dat.lib.metadata;

import io.shinhanlife.dat.lib.util.ToolScaffolder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class V17ToolSchemaPolicyTest {

    @Test
    void removesCategoryPrefixFromPublishedThreePartName() {
        assertEquals("SearchFund",
                V17ToolSchemaPolicy.normalizeJavaBaseName("pro_search_fund", "pro"));
        assertEquals("DetailContract",
                V17ToolSchemaPolicy.normalizeJavaBaseName("ProDetailContract", "pro"));
    }

    @Test
    void rejectsUnsupportedReadActionAndInvalidMetadataCardinality() {
        assertThrows(IllegalArgumentException.class,
                () -> V17ToolSchemaPolicy.normalizeJavaBaseName("ProInquiryFund", "pro"));
        assertThrows(IllegalArgumentException.class,
                () -> V17ToolSchemaPolicy.validateExamples(List.of("하나", "둘")));
        assertThrows(IllegalArgumentException.class,
                () -> V17ToolSchemaPolicy.validateTags(List.of("pro", "펀드"), "pro"));
    }

    @Test
    void preservesPagingFieldNamesAndMakesThemOptional() {
        List<ToolScaffolder.FieldDefinition> normalized = V17ToolSchemaPolicy.normalizePagingFields(List.of(
                new ToolScaffolder.FieldDefinition("scrollPaging", "String", "", List.of(), "", true),
                new ToolScaffolder.FieldDefinition("scrPageInfo", "String", "", List.of(), "", true),
                new ToolScaffolder.FieldDefinition("pageInfo", "String", "", List.of(), "", true)));

        assertEquals("scrollPaging", normalized.get(0).name());
        assertEquals("scrPageInfo", normalized.get(1).name());
        assertEquals("pageInfo", normalized.get(2).name());
        assertFalse(normalized.get(1).required());
        assertFalse(normalized.get(2).required());
        assertEquals("페이징 처리 객체 (생략 시 시스템 기본값 적용)", normalized.get(1).description());
    }
}
