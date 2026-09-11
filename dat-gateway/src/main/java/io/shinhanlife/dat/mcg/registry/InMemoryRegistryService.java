package io.shinhanlife.dat.mcg.registry;

import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @package io.shinhanlife.dat.mcg.registry
 * @className InMemoryRegistryService
 * @description AX HUB 인메모리 툴 레지스트리
 * @author 0986406
 * @create 2026.09.01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InMemoryRegistryService {

    private final Map<String, ToolMetadata> toolCache = new ConcurrentHashMap<>();
    private static final long DEFAULT_TTL_MILLIS = 45000; // 45초

    /**
     * 툴 등록 및 갱신
     */
    public void saveTool(ToolMetadata meta) {
        toolCache.put(meta.getUid(), meta);
        log.info(" [InMemoryRegistry] 툴 등록 완료: {}", meta.getUid());
    }

    public List<String> getAvailablePods(String uid) {
        ToolMetadata tool = getTool(uid);
        if (tool != null && tool.getPodUrl() != null) {
            return List.of(tool.getPodUrl());
        }
        return List.of();
    }

    /**
     * 실행 시 툴 정보 조회
     */
    public ToolMetadata getTool(String uid) {
        return toolCache.get(uid);
    }

    /**
     * 툴 이름으로 정보 조회
     */
    public ToolMetadata getToolByName(String name) {
        for (ToolMetadata tool : toolCache.values()) {
            if (name.equals(tool.getName())) {
                return tool;
            }
        }
        return null;
    }

    /**
     * 등록된 모든 활성 툴 목록 조회
     */
    public List<ToolMetadata> getAllTools() {
        return List.copyOf(toolCache.values());
    }

    /**
     * 툴 명시적 제거 (Deregister)
     */
    public void removeTool(String uid) {
        toolCache.remove(uid);
        log.info(" [InMemoryRegistry] 툴 삭제 완료: {}", uid);
    }
}