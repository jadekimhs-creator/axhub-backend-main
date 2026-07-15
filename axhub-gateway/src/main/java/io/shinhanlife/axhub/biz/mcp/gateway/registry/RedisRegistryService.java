package io.shinhanlife.axhub.biz.mcp.gateway.registry;

import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * @package io.shinhanlife.axhub.biz.mcp.gateway.registry
 * @className RedisRegistryService
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
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRegistryService {

    private final RedisTemplate<String, ToolMetadata> redisTemplate;
    private static final String KEY_PREFIX = "mcp:tool:";
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(45); // 실무 하트비트 주기 반영

    /**
     * 툴 등록 및 갱신 (TTL 기반으로 60초 뒤 자동 만료)
     */
    public void saveTool(ToolMetadata meta) {
        String key = KEY_PREFIX + meta.getUid();
        meta.setLastHeartbeat(System.currentTimeMillis());
        redisTemplate.opsForValue().set(key, meta, DEFAULT_TTL);
        log.info(" [RedisRegistry] 툴 등록 완료: {}", meta.getUid());
    }

    /**
     * 하트비트 갱신 (TTL 초기화)
     */
    public boolean refreshHeartbeat(String uid) {
        String key = KEY_PREFIX + uid;
        Boolean exists = redisTemplate.expire(key, DEFAULT_TTL);
        if (Boolean.TRUE.equals(exists)) {
            log.debug(" [RedisRegistry] 하트비트 갱신: {}", uid);
            return true;
        } else {
            log.warn(" [RedisRegistry] 존재하지 않는 툴에 대한 하트비트 요청: {}", uid);
            return false;
        }
    }
    public List<String> getAvailablePods(String uid) {
        ToolMetadata tool = getTool(uid);

        if (tool != null && tool.getPodUrl() != null) {
            return List.of(tool.getPodUrl());
        }
        
        return List.of();
    }
    /**
     * 실행 시 툴 정보 조회 (Tool Execution 시 참조)
     */
    public ToolMetadata getTool(String uid) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + uid);
    }

    /**
     * 툴 이름으로 정보 조회
     */
    public ToolMetadata getToolByName(String name) {
        List<ToolMetadata> allTools = getAllTools();
        for (ToolMetadata tool : allTools) {
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
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<ToolMetadata> tools = redisTemplate.opsForValue().multiGet(keys);
        return tools != null ? tools.stream().filter(Objects::nonNull).collect(Collectors.toList()) : List.of();
    }

    /**
     * 툴 명시적 제거 (Deregister)
     */
    public void removeTool(String uid) {
        redisTemplate.delete(KEY_PREFIX + uid);
        log.info(" [RedisRegistry] 툴 삭제 완료: {}", uid);
    }
}