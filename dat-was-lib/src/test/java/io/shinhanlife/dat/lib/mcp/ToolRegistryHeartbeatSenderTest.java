package io.shinhanlife.dat.lib.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dat.lib.annotation.GrowToolHint;
import io.shinhanlife.dat.lib.config.McpProperties;
import io.shinhanlife.dat.lib.metadata.ToolDefinition;
import io.shinhanlife.dat.lib.metadata.ToolDefinitionRepository;
import io.shinhanlife.dat.lib.metadata.ToolDescription;
import io.shinhanlife.dat.lib.util.ToolSchemaResolver;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.context.ApplicationContext;

class ToolRegistryHeartbeatSenderTest {

    @Test
    void excludesRegisterFalseToolFromGatewayRegistrationTargets() throws Exception {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(Object.class)).thenReturn(Map.of("disabledTool", new DisabledTool()));
        ToolRegistryHeartbeatSender sender = new ToolRegistryHeartbeatSender(
                applicationContext, new ObjectMapper(), new McpProperties(), mock(ToolSchemaResolver.class));

        setField(sender, "podUrl", "http://localhost:8084");

        sender.init();

        assertThat(sender.getAllScannedTools()).singleElement()
                .extracting(tool -> tool.getIsRegistered())
                .isEqualTo(false);
        assertThat(registeredTools(sender)).isEmpty();
    }

    @Test
    void keepsOnlyFunctionDescriptionAndStoresUsageGuidanceSeparately() throws Exception {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(Object.class)).thenReturn(Map.of("tool", new EnabledTool()));
        ToolDefinitionRepository definitions = mock(ToolDefinitionRepository.class);
        when(definitions.findByName("test_enabled_tool")).thenReturn(java.util.Optional.of(new ToolDefinition(
                "test_enabled_tool", "테스트 도구", "1.0.0", "test",
                new ToolDescription("기능 설명", "사용 시점", "사용 제외", "입출력 제한"),
                "도구 설명", List.of("질문 1", "질문 2", "질문 3"), true, false, true,
                Map.of("type", "object", "properties", Map.of(), "additionalProperties", false), null,
                List.of("test"), null, List.of(), "MCP_TOOL")));
        ToolRegistryHeartbeatSender sender = new ToolRegistryHeartbeatSender(applicationContext, new ObjectMapper(),
                new McpProperties(), mock(ToolSchemaResolver.class), definitions);

        sender.init();

        assertThat(sender.getAllScannedTools()).singleElement().satisfies(tool -> {
            assertThat(tool.getDescription()).isEqualTo("기능 설명");
            assertThat(tool.getWhenToUse()).isEqualTo("사용 시점");
            assertThat(tool.getWhenNotToUse()).isEqualTo("사용 제외");
            assertThat(tool.getIoLimits()).isEqualTo("입출력 제한");
        });
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
    @SuppressWarnings("unchecked")
    private List<?> registeredTools(ToolRegistryHeartbeatSender sender) throws Exception {
        Field field = ToolRegistryHeartbeatSender.class.getDeclaredField("registeredTools");
        field.setAccessible(true);
        return (List<?>) field.get(sender);
    }

    static class DisabledTool {

        @McpTool(name = "test_disabled_tool")
        @GrowToolHint
        void execute() {
        }
    }

    static class EnabledTool {
        @McpTool(name = "test_enabled_tool")
        void execute() {
        }
    }
}
