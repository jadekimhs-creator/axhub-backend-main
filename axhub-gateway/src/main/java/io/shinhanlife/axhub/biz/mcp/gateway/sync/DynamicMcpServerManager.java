package io.shinhanlife.axhub.biz.mcp.gateway.sync;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcSseServerTransportProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @package io.shinhanlife.axhub.biz.mcp.gateway.sync
 * @className DynamicMcpServerManager
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
@Component
public class DynamicMcpServerManager {
    private static final Logger log = LoggerFactory.getLogger(DynamicMcpServerManager.class);

    private final Map<String, McpSyncServer> categoryServers = new ConcurrentHashMap<>();
    private final Map<String, WebMvcSseServerTransportProvider> categoryTransports = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> managedToolNamesPerCategory = new ConcurrentHashMap<>();

    private final RegistryMcpToolSpecificationFactory specificationFactory;

    public DynamicMcpServerManager(RegistryMcpToolSpecificationFactory specificationFactory) {
        this.specificationFactory = specificationFactory;
    }

    public void synchronizeCategory(String categoryKey, List<ToolMetadata> tools) {
        if (categoryKey == null || categoryKey.trim().isEmpty()) {
            categoryKey = "common";
        }
        String safeCategory = categoryKey.toLowerCase();
        
        McpSyncServer server = categoryServers.computeIfAbsent(safeCategory, key -> {
            log.info("Creating dynamic MCP Server for category: {}", key);
            String ssePath = "common".equals(key) ? "/mcp/sse" : "/mcp/sse/" + key;
            String msgPath = "common".equals(key) ? "/mcp/message" : "/mcp/message/" + key;
            
            WebMvcSseServerTransportProvider transport = WebMvcSseServerTransportProvider.builder()
                .sseEndpoint(ssePath)
                .messageEndpoint(msgPath)
                .build();
                
            McpSyncServer newServer = McpServer.sync(transport)
                .serverInfo("AXHUB-Gateway-" + key, "1.0.0")
                .build();
                
            categoryTransports.put(key, transport);
            managedToolNamesPerCategory.put(key, ConcurrentHashMap.newKeySet());
            return newServer;
        });

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

    public RouterFunction<ServerResponse> getDynamicRouter() {
        return request -> {
            for (WebMvcSseServerTransportProvider transport : categoryTransports.values()) {
                Optional<HandlerFunction<ServerResponse>> handler = transport.getRouterFunction().route(request);
                if (handler.isPresent()) {
                    return handler;
                }
            }
            return Optional.empty();
        };
    }
}
