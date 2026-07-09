package io.shinhanlife.axhub.biz.mcp.gateway.presentation;

import io.shinhanlife.axhub.biz.mcp.gateway.service.KillSwitchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.gateway.presentation
 * @className KillSwitchController
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
@RestController
@RequestMapping("/mcp/api/v1/admin/kill-switch")
@RequiredArgsConstructor
public class KillSwitchController {

    private final KillSwitchService killSwitchService;

    /**
     * 관리자가 비상 차단(Kill Switch) 상태를 토글합니다.
     * @param type "agent", "tool", "route" 중 하나
     * @param target 차단할 대상의 ID 또는 이름
     * @param state true(차단), false(차단 해제)
     */
    @PostMapping("/{type}/{target}")
    public ResponseEntity<?> toggleKillSwitch(
            @PathVariable String type,
            @PathVariable String target,
            @RequestParam boolean state) {
            
        killSwitchService.toggleKillSwitch(type, target, state);
        
        return ResponseEntity.ok(Map.of(
            "message", "Kill Switch 상태 변경 완료",
            "type", type,
            "target", target,
            "state", state
        ));
    }

    /**
     * 관리자가 비상 차단(Kill Switch) 키를 Redis에서 완전히 삭제합니다.
     */
    @DeleteMapping("/{type}/{target}")
    public ResponseEntity<?> removeKillSwitch(
            @PathVariable String type,
            @PathVariable String target) {
            
        killSwitchService.removeKillSwitch(type, target);
        
        return ResponseEntity.ok(Map.of(
            "message", "Kill Switch 키 삭제 완료 (차단 해제)",
            "type", type,
            "target", target
        ));
    }
}