package io.shinhanlife.axhub.biz.mcp.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @package io.shinhanlife.axhub.biz.mcp.gateway.service
 * @className KillSwitchService
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
public class KillSwitchService {

    // 기본 제공되는 StringRedisTemplate 사용
    private final StringRedisTemplate stringRedisTemplate;

    public void checkAgent(String tenantId) {
        String key = "mcp:kill:agent:" + tenantId;
        if ("true".equalsIgnoreCase(stringRedisTemplate.opsForValue().get(key))) {
            log.warn(" [KillSwitch] 차단된 에이전트 접근 시도: {}", tenantId);
            throw new SecurityException(" 비상 차단: 해당 테넌트(" + tenantId + ")의 접근이 관리자에 의해 차단되었습니다.");
        }
    }

    public void checkTool(String toolName) {
        if (toolName == null || toolName.isBlank()) return;
        
        String key = "mcp:kill:tool:" + toolName;
        if ("true".equalsIgnoreCase(stringRedisTemplate.opsForValue().get(key))) {
            log.warn(" [KillSwitch] 차단된 툴 실행 시도: {}", toolName);
            throw new SecurityException(" 비상 차단: 해당 툴(" + toolName + ")의 실행이 관리자에 의해 차단되었습니다.");
        }
    }

    public void checkRoute(String integrationType) {
        if (integrationType == null || integrationType.isBlank()) return;
        
        String key = "mcp:kill:route:" + integrationType;
        if ("true".equalsIgnoreCase(stringRedisTemplate.opsForValue().get(key))) {
            log.warn(" [KillSwitch] 차단된 라우트 통신 시도: {}", integrationType);
            throw new SecurityException(" 비상 차단: 해당 레거시 라우트(" + integrationType + ")로의 통신이 관리자에 의해 차단되었습니다.");
        }
    }
    
    // 관리자 API용 토글 메서드
    public void toggleKillSwitch(String type, String target, boolean state) {
        String key = "mcp:kill:" + type + ":" + target;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(state));
        log.info(" [KillSwitch] 상태 변경: {} -> {} (차단 상태: {})", type, target, state);
    }

    // 관리자 API용 삭제 메서드 (Redis 키 자체를 완전 삭제)
    public void removeKillSwitch(String type, String target) {
        String key = "mcp:kill:" + type + ":" + target;
        stringRedisTemplate.delete(key);
        log.info(" [KillSwitch] 차단 키 완전 삭제: {} -> {}", type, target);
    }
}