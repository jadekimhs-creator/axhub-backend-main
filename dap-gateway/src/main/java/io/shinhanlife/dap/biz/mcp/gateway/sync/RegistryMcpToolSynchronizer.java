package io.shinhanlife.dap.biz.mcp.gateway.sync;

import io.shinhanlife.dap.biz.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.dap.biz.mcp.gateway.registry.RedisRegistryService;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @package io.shinhanlife.dap.biz.mcp.gateway.sync
 * @className RegistryMcpToolSynchronizer
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
@Service
public class RegistryMcpToolSynchronizer {
    private static final Logger log = LoggerFactory.getLogger(RegistryMcpToolSynchronizer.class);

    private final ObjectProvider<McpSyncServer> mcpServerProvider;
    private final DynamicMcpServerManager dynamicMcpServerManager;
    private final RedisRegistryService redisRegistryService;
    private final RegistryMcpToolSpecificationFactory specificationFactory;
    private final Set<String> managedToolNames = new LinkedHashSet<>();
    private final ReentrantLock lock = new ReentrantLock();

    public RegistryMcpToolSynchronizer(ObjectProvider<McpSyncServer> mcpServerProvider,
                                       DynamicMcpServerManager dynamicMcpServerManager,
                                       RedisRegistryService redisRegistryService,
                                       RegistryMcpToolSpecificationFactory specificationFactory) {
        this.mcpServerProvider = mcpServerProvider;
        this.dynamicMcpServerManager = dynamicMcpServerManager;
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
        lock.lock();
        try {
            List<ToolMetadata> activeEntries = redisRegistryService.getAllTools()
                    .stream().filter(ToolMetadata::getVisible).collect(Collectors.toList());
            
            // 1. Dynamic MCP Server 동기화 (카테고리별)
            Map<String, List<ToolMetadata>> toolsByCategory = activeEntries.stream()
                    .collect(Collectors.groupingBy(
                            t -> (t.getCategoryKey() == null || t.getCategoryKey().trim().isEmpty()) ? "common" : t.getCategoryKey()
                    ));
            
            Set<String> allKnownCategories = new LinkedHashSet<>(dynamicMcpServerManager.getKnownCategories());
            allKnownCategories.addAll(toolsByCategory.keySet());
            
            for (String category : allKnownCategories) {
                dynamicMcpServerManager.synchronizeCategory(category, toolsByCategory.getOrDefault(category, List.of()));
            }

            // 2. Global MCP Server 동기화 (기존 하위 호환)
            McpSyncServer server = mcpServerProvider.getIfAvailable();
            if (server != null) {
                Set<String> activeNames = activeEntries.stream()
                        .map(ToolMetadata::getName)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                Set<String> removedNames = new LinkedHashSet<>(managedToolNames);
                removedNames.removeAll(activeNames);
                for (String removedName : removedNames) {
                    server.removeTool(removedName);
                    managedToolNames.remove(removedName);
                    log.info("Removed MCP registry tool (Global). tool={}", removedName);
                }

                for (ToolMetadata entry : activeEntries) {
                    if (managedToolNames.contains(entry.getName())) {
                        server.removeTool(entry.getName());
                    }
                    server.addTool(specificationFactory.create(entry));
                    managedToolNames.add(entry.getName());
                }
            }
        } catch (Exception error) {
            log.warn("MCP registry tool synchronization failed. message={}", error.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
