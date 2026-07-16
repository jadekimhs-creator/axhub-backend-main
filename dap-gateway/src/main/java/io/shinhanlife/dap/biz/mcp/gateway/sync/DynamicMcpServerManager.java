package io.shinhanlife.dap.biz.mcp.gateway.sync;

import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.shinhanlife.dap.biz.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.dap.biz.mcp.gateway.sync.CustomWebMvcSseServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class DynamicMcpServerManager {
    private static final Logger log = LoggerFactory.getLogger(DynamicMcpServerManager.class);

    private final Map<String, McpSyncServer> categoryServers = new ConcurrentHashMap<>();
    private final Map<String, CustomWebMvcSseServerTransportProvider> categoryTransports = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> managedToolNamesPerCategory = new ConcurrentHashMap<>();

    private final RegistryMcpToolSpecificationFactory specificationFactory;
    private final ObjectMapper objectMapper;

    public DynamicMcpServerManager(RegistryMcpToolSpecificationFactory specificationFactory, ObjectMapper objectMapper) {
        this.specificationFactory = specificationFactory;
        this.objectMapper = objectMapper;

        // Pre-initialize basic categories so their endpoints are always open
        getOrCreateServer("common");
        getOrCreateServer("hr");
        getOrCreateServer("payment");
        getOrCreateServer("sms");
        getOrCreateServer("email");
        getOrCreateServer("other");
    }

    private McpSyncServer getOrCreateServer(String categoryKey) {
        String safeCategory = (categoryKey == null || categoryKey.trim().isEmpty()) ? "common" : categoryKey.toLowerCase();

        return categoryServers.computeIfAbsent(safeCategory, key -> {
            log.info("Creating dynamic MCP Server for category: {}", key);
            String ssePath = "/mcp/sse/" + key;
            String msgPath = "/mcp/message/" + key;

            CustomWebMvcSseServerTransportProvider transport = new CustomWebMvcSseServerTransportProvider(ssePath, msgPath, objectMapper);

            McpSyncServer newServer = McpServer.sync(transport)
                .serverInfo("DAP-Gateway-" + key, "1.0.0")
                .capabilities(ServerCapabilities.builder().tools(true).build())
                .build();
                
            categoryTransports.put(key, transport);
            managedToolNamesPerCategory.put(key, ConcurrentHashMap.newKeySet());
            return newServer;
        });
    }

    public void synchronizeCategory(String categoryKey, List<ToolMetadata> tools) {
        String safeCategory = (categoryKey == null || categoryKey.trim().isEmpty()) ? "common" : categoryKey.toLowerCase();
        McpSyncServer server = getOrCreateServer(safeCategory);

        Set<String> managedToolNames = managedToolNamesPerCategory.get(safeCategory);
        Set<String> activeNames = tools.stream()
                .map(ToolMetadata::getName)
                .collect(Collectors.toSet());

        Set<String> removedNames = managedToolNames.stream()
                .filter(name -> !activeNames.contains(name))
                .collect(Collectors.toSet());

        for (String removedName : removedNames) {
            server.removeTool(removedName);
            managedToolNames.remove(removedName);
            log.info("[{}] Removed tool: {}", safeCategory, removedName);
        }

        for (ToolMetadata entry : tools) {
            if (!managedToolNames.contains(entry.getName())) {
                server.addTool(specificationFactory.create(entry));
                managedToolNames.add(entry.getName());
                log.info("[{}] Added tool: {}", safeCategory, entry.getName());
            } else {
                server.removeTool(entry.getName());
                server.addTool(specificationFactory.create(entry));
            }
        }
    }

    public Set<String> getKnownCategories() {
        return Collections.unmodifiableSet(categoryServers.keySet());
    }

    public CustomWebMvcSseServerTransportProvider getTransport(String categoryKey) {
        String safeCategory = (categoryKey == null || categoryKey.trim().isEmpty()) ? "common" : categoryKey.toLowerCase();
        return categoryTransports.get(safeCategory);
    }
}
