package io.shinhanlife.dat.mcg.trace;


/**
 * @package io.shinhanlife.dat.mcg.trace
 * @className McpMonitorEventService
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
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class McpMonitorEventService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * MCP Monitor browser screen subscribes to this emitter.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        return emitter;
    }

    /**
     * Sends one monitor event only when an Agent tool request changes state inside MCP.
     */
    public void publish(String payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("mcp-tool-trace")
                        .data(payload));
            } catch (IOException | IllegalStateException error) {
                emitters.remove(emitter);
            }
        }
    }
}
