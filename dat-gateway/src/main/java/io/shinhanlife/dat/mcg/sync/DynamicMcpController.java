package io.shinhanlife.dat.mcg.sync;


/**
 * @package io.shinhanlife.dat.mcg.sync
 * @className DynamicMcpController
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 * 
 * </pre>
 */
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import io.shinhanlife.dat.mcg.sync.CustomWebMvcSseServerTransportProvider;

@RestController
public class DynamicMcpController {

    private final DynamicMcpServerManager manager;

    public DynamicMcpController(DynamicMcpServerManager manager) {
        this.manager = manager;
    }

    @GetMapping("/mcp/sse/{category}")
    public SseEmitter handleSse(@PathVariable("category") String category) {
        CustomWebMvcSseServerTransportProvider transport = manager.getTransport(category);
        if (transport == null) {
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        return transport.handleSse();
    }

    @PostMapping("/mcp/message/{category}")
    public ResponseEntity<String> handleMessage(
            @PathVariable("category") String category,
            @RequestParam("sessionId") String sessionId,
            @RequestBody String body) {
            
        CustomWebMvcSseServerTransportProvider transport = manager.getTransport(category);
        if (transport == null) {
            return ResponseEntity.badRequest().body("Unknown category: " + category);
        }
        return transport.handleMessage(sessionId, body);
    }

    @PostMapping("/mcp/custom/{category}")
    public ResponseEntity<?> handleCustomMcp(
            @PathVariable("category") String category,
            @RequestHeader(value = "Mcp-Session-Id", required = false) String sessionId,
            @RequestBody(required = false) String body) {
            
        CustomWebMvcSseServerTransportProvider transport = manager.getTransport(category);
        if (transport == null) {
            return ResponseEntity.badRequest().body("Unknown category: " + category);
        }

        boolean isNew = (sessionId == null || sessionId.isEmpty());
        String activeSessionId = isNew ? java.util.UUID.randomUUID().toString() : sessionId;

        if (!isNew && !transport.hasSession(activeSessionId)) {
            // 클라이언트가 보낸 세션 ID가 만료되었거나 존재하지 않는 경우 (Warm Pool 스펙: 404 Not Found 반환)
            return ResponseEntity.notFound().build();
        }

        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = transport.handleCustomSse(activeSessionId, body);

        return ResponseEntity.ok()
                .header("Mcp-Session-Id", activeSessionId)
                .body(emitter);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/mcp/sse/{category}")
    public ResponseEntity<Void> handleDeleteSse(@PathVariable("category") String category) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/.well-known/oauth-protected-resource")
    public ResponseEntity<java.util.Map<String, Object>> handleOauthProtectedResource() {
        return ResponseEntity.ok().body(java.util.Map.of());
    }

    @GetMapping("/.well-known/oauth-protected-resource/**")
    public ResponseEntity<java.util.Map<String, Object>> handleOauthProtectedResourceWildcard() {
        return ResponseEntity.ok().body(java.util.Map.of());
    }
}
