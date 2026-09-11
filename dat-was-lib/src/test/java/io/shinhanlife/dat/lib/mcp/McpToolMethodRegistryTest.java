package io.shinhanlife.dat.lib.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.shinhanlife.dat.lib.config.McpProperties;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.context.support.StaticApplicationContext;

class McpToolMethodRegistryTest {

    @Test
    void indexesAnnotatedToolDuringInitialization() {
        StaticApplicationContext context = contextWith(new EchoTool());
        McpToolMethodRegistry registry = new McpToolMethodRegistry(context, new McpProperties());

        registry.initialize();

        McpToolMethodRegistry.RegisteredTool tool = registry.find("cmm_echo_search");
        assertNotNull(tool);
        assertEquals("execute", tool.method().getName());
        assertEquals("cmm_echo_search", tool.annotation().name());
    }

    @Test
    void rejectsDuplicateToolNamesDuringInitialization() {
        StaticApplicationContext context = contextWith(new EchoTool(), new DuplicateEchoTool());
        McpToolMethodRegistry registry = new McpToolMethodRegistry(context, new McpProperties());

        assertThrows(IllegalStateException.class, registry::initialize);
    }

    private StaticApplicationContext contextWith(Object... tools) {
        StaticApplicationContext context = new StaticApplicationContext();
        for (int index = 0; index < tools.length; index++) {
            context.getBeanFactory().registerSingleton("tool" + index, tools[index]);
        }
        context.refresh();
        return context;
    }

    static class EchoTool {
        @McpTool(name = "cmm_echo_search")
        public String execute(String request) {
            return request;
        }
    }

    static class DuplicateEchoTool {
        @McpTool(name = "cmm_echo_search")
        public String execute(String request) {
            return request;
        }
    }
}