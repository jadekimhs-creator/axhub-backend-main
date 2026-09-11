package io.shinhanlife.dat.lib.mcp;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/** Exposes every Tool Pod through the MCP Streamable HTTP transport. */
@Slf4j
@Configuration
public class ToolMcpServerConfiguration {

    @PostConstruct
    public void init() {
        log.warn("=================================================");
        log.warn("ToolMcpServerConfiguration IS LOADED BY SPRING!!!");
        log.warn("=================================================");
    }

    @Bean
    @Primary
    public HttpServletStreamableServerTransportProvider toolMcpTransportProvider() {
        return HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint("/mcp")
                .build();
    }

    /** Creates the Tool Pod MCP server directly with MCP Java SDK 2.0.0. */
    @Bean(destroyMethod = "close")
    public McpSyncServer toolMcpServer(HttpServletStreamableServerTransportProvider transportProvider) {
        return McpServer.sync(transportProvider)
                .serverInfo("dap-tool-pod", "2.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletStreamableServerTransportProvider> toolMcpServlet(
            HttpServletStreamableServerTransportProvider transportProvider) {
        // "/mcp/*"로 매핑하면 BusinessToolController의 "/mcp/api/v1/tools/local" 까지 가로채게 되므로,
        // 정확히 MCP 통신에 사용되는 "/mcp" 와 "/mcp/message" 두 개만 매핑합니다.
        ServletRegistrationBean<HttpServletStreamableServerTransportProvider> registration = 
                new ServletRegistrationBean<>(transportProvider, "/mcp", "/mcp/message");
        registration.setAsyncSupported(true);
        return registration;
    }
}
