package io.shinhanlife.dat.mcg.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @package io.shinhanlife.dat.mcg.service
 * @className KillSwitchService
 * @description AX HUB 시스템 킬 스위치 서비스 (인메모리)
 * @author 0986406
 * @create 2026.09.01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KillSwitchService {

    // 인메모리 맵으로 변경
    private final Map<String, String> killSwitchMap = new ConcurrentHashMap<>();

    public void checkAgent(String tenantId) {
        String key = "mcp:kill:agent:" + tenantId;
        if ("true".equalsIgnoreCase(killSwitchMap.get(key))) {
            log.warn(" [KillSwitch] 차단된 에이전트 접근 시도: {}", tenantId);
            throw new SecurityException(" 비상 차단: 해당 테넌트(" + tenantId + ")의 접근이 관리자에 의해 차단되었습니다.");
        }
    }

    public void checkTool(String toolName) {
        if (toolName == null || toolName.isBlank()) return;
        
        String key = "mcp:kill:tool:" + toolName;
        if ("true".equalsIgnoreCase(killSwitchMap.get(key))) {
            log.warn(" [KillSwitch] 차단된 툴 실행 시도: {}", toolName);
            throw new SecurityException(" 비상 차단: 해당 툴(" + toolName + ")의 실행이 관리자에 의해 차단되었습니다.");
        }
    }

    public void checkRoute(String integrationType) {
        if (integrationType == null || integrationType.isBlank()) return;
        
        String key = "mcp:kill:route:" + integrationType;
        if ("true".equalsIgnoreCase(killSwitchMap.get(key))) {
            log.warn(" [KillSwitch] 차단된 라우트 통신 시도: {}", integrationType);
            throw new SecurityException(" 비상 차단: 해당 레거시 라우트(" + integrationType + ")로의 통신이 관리자에 의해 차단되었습니다.");
        }
    }
    
    // 관리자 API용 토글 메서드
    public void toggleKillSwitch(String type, String target, boolean state) {
        String key = "mcp:kill:" + type + ":" + target;
        killSwitchMap.put(key, String.valueOf(state));
        log.info(" [KillSwitch] 상태 변경: {} -> {} (차단 상태: {})", type, target, state);
    }

    // 관리자 API용 삭제 메서드 (맵 항목 삭제)
    public void removeKillSwitch(String type, String target) {
        String key = "mcp:kill:" + type + ":" + target;
        killSwitchMap.remove(key);
        log.info(" [KillSwitch] 차단 키 완전 삭제: {} -> {}", type, target);
    }
}