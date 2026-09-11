package io.shinhanlife.dat.mcc.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.server.McpSyncServer;
import io.shinhanlife.dat.lib.mcp.ToolMcpServerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

class ToolMcpServerConfigurationTest {

    private final ToolMcpServerConfiguration configuration = new ToolMcpServerConfiguration();

    @Test
    void exposesOnlyExactMcpEndpointSoLegacyMcpApiPathsRemainAvailable() {
        HttpServletStreamableServerTransportProvider transport = configuration.toolMcpTransportProvider();
        ServletRegistrationBean<HttpServletStreamableServerTransportProvider> registration = configuration.toolMcpServlet(transport);

        assertThat(registration.getUrlMappings()).containsExactlyInAnyOrder("/mcp", "/mcp/message");
    }

    @Test
    void createsMcpSdkTwoSyncServerWithoutSpringAiAutoConfiguration() {
        HttpServletStreamableServerTransportProvider transport = configuration.toolMcpTransportProvider();

        McpSyncServer server = configuration.toolMcpServer(transport);

        assertEquals("dap-tool-pod", server.getServerInfo().name());
        server.close();
    }
}
