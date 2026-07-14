package io.shinhanlife.axhub.biz.mcp.gateway.sync;

import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.gateway.registry.RedisRegistryService;
import io.modelcontextprotocol.server.McpSyncServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * MCP 서버의 tools/list 노출 목록을 Redis Registry의 ACTIVE Tool 목록과 동기화합니다.
 */
@Service
public class RegistryMcpToolSynchronizer {
    private static final Logger log = LoggerFactory.getLogger(RegistryMcpToolSynchronizer.class);

    private final ObjectProvider<McpSyncServer> mcpServerProvider;
    private final RedisRegistryService redisRegistryService;
    private final RegistryMcpToolSpecificationFactory specificationFactory;
    private final Set<String> managedToolNames = new LinkedHashSet<>();
    private final ReentrantLock lock = new ReentrantLock();

    public RegistryMcpToolSynchronizer(ObjectProvider<McpSyncServer> mcpServerProvider,
                                       RedisRegistryService redisRegistryService,
                                       RegistryMcpToolSpecificationFactory specificationFactory) {
        this.mcpServerProvider = mcpServerProvider;
        this.redisRegistryService = redisRegistryService;
        this.specificationFactory = specificationFactory;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeOnStartup() {
        synchronize();
    }

    @Scheduled(fixedDelayString = "${mcp.tool-registry-sync-interval-millis:5000}")
    public void synchronizeScheduled() {
        synchronize();
    }

    public void synchronize() {
        McpSyncServer server = mcpServerProvider.getIfAvailable();
        if (server == null) {
            return;
        }

        lock.lock();
        try {
            List<ToolMetadata> activeEntries = redisRegistryService.getAllTools()
                    .stream().filter(ToolMetadata::getVisible).collect(Collectors.toList());
                    
            Set<String> activeNames = activeEntries.stream()
                    .map(ToolMetadata::getName) // We use 'name' for MCP Tool Name
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Set<String> removedNames = new LinkedHashSet<>(managedToolNames);
            removedNames.removeAll(activeNames);
            for (String removedName : removedNames) {
                server.removeTool(removedName);
                managedToolNames.remove(removedName);
                log.info("Removed MCP registry tool. tool={}", removedName);
            }

            for (ToolMetadata entry : activeEntries) {
                if (managedToolNames.contains(entry.getName())) {
                    server.removeTool(entry.getName());
                }
                server.addTool(specificationFactory.create(entry));
                managedToolNames.add(entry.getName());
            }
        } catch (Exception error) {
            log.warn("MCP registry tool synchronization failed. message={}", error.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
