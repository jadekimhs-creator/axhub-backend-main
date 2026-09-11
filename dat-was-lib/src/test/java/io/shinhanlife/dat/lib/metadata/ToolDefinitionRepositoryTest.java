package io.shinhanlife.dat.lib.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;

class ToolDefinitionRepositoryTest {

    @Test
    void springCanCreateRepositoryWithoutDefaultConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(ToolDefinitionRepository.class);
            context.refresh();

            assertTrue(context.getBean(ToolDefinitionRepository.class)
                    .findByName("cmm_claim_search").isPresent());
        }
    }

    @Test
    void loadsAndCachesAValidV17DefinitionByToolName() {
        ToolDefinitionRepository repository = new ToolDefinitionRepository(
                new ObjectMapper(new YAMLFactory()), new DefaultResourceLoader(),
                "classpath*:tool-definitions/**/*.yml");

        ToolDefinition definition = repository.findByName("cmm_claim_search").orElseThrow();

        assertEquals("보험금 청구 상태 조회", definition.displayName());
        assertEquals("cmm", definition.categoryKey());
        assertEquals(3, definition.exampleQueries().size());
        assertEquals(false, definition.parametersSchema().get("additionalProperties"));
    }

    @Test
    void rejectsDefinitionWithFewerThanThreeExampleQueries() {
        ToolDefinition invalid = validDefinition().withExampleQueries(java.util.List.of("청구 상태 알려줘"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ToolDefinitionValidator.validate(invalid, "memory:invalid"));

        assertTrue(error.getMessage().contains("example_queries"));
    }

    @Test
    void rejectsNonStandardToolName() {
        ToolDefinition invalid = validDefinition().withName("cmm_claim.Search");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ToolDefinitionValidator.validate(invalid, "memory:invalid"));

        assertTrue(error.getMessage().contains("name"));
    }

    private ToolDefinition validDefinition() {
        return new ToolDefinition(
                "cmm_claim_search", "보험금 청구 상태 조회", "1.0.0", "cmm",
                new ToolDescription("청구 상태를 조회한다.", "상태 확인 시 사용한다.",
                        "청구 접수 시 사용하지 않는다.", "청구번호가 필요하다."),
                "보험금 청구 상태를 조회합니다.",
                java.util.List.of("청구 상태 알려줘", "심사 결과 조회해줘", "계약번호로 청구를 찾아줘"),
                true, false, true,
                java.util.Map.of("type", "object", "properties", java.util.Map.of(),
                        "additionalProperties", false),
                null,
                java.util.List.of("보험금"), null, java.util.List.of(), "MCP_TOOL");
    }
}
