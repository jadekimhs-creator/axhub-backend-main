package io.shinhanlife.dap.biz.mcp.gateway.sync;

/**
 * @package io.shinhanlife.dap.biz.mcp.gateway.sync
 * @className CustomWebMvcSseServerTransportProvider
 * @description AX HUB MCP Gateway SSE 전송 제공자 - SSE 기반의 MCP 서버 트랜스포트를 구현하는 클래스
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCMessage;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCNotification;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCRequest;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCResponse;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RequestPredicates.accept;

@Slf4j
public class CustomWebMvcSseServerTransportProvider implements McpServerTransportProvider {

    private McpServerSession.Factory sessionFactory;
    private final String sseEndpoint;
    private final String messageEndpoint;
    private final Map<String, McpServerSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public CustomWebMvcSseServerTransportProvider(String sseEndpoint, String messageEndpoint, ObjectMapper objectMapper) {
        this.sseEndpoint = sseEndpoint;
        this.messageEndpoint = messageEndpoint;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            sessions.values().forEach(session -> {
                try {
                    session.closeGracefully().subscribe();
                } catch (Exception ignored) {}
            });
            sessions.clear();
        });
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        return Mono.when(sessions.values().stream()
                .map(session -> session.sendNotification(method, params))
                .toList());
    }

    public SseEmitter handleSse() {
        if (sessionFactory == null) {
            throw new IllegalStateException("SessionFactory not configured");
        }

        SseEmitter emitter = new SseEmitter(-1L);
        String sessionId = UUID.randomUUID().toString();

        CustomMcpSessionTransport sessionTransport = new CustomMcpSessionTransport(emitter, sessionId);
        McpServerSession session = sessionFactory.create(sessionTransport);
        sessions.put(sessionId, session);

        emitter.onCompletion(() -> sessions.remove(sessionId));
        emitter.onTimeout(() -> sessions.remove(sessionId));

        new Thread(() -> {
            try {
                Thread.sleep(100);
                emitter.send(SseEmitter.event().name("endpoint").data(messageEndpoint + "?sessionId=" + sessionId));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    public SseEmitter handleCustomSse(String sessionId, String body) {
        if (sessionFactory == null) {
            throw new IllegalStateException("SessionFactory not configured");
        }

        SseEmitter emitter = new SseEmitter(-1L);

        CustomMcpSessionTransport sessionTransport = new CustomMcpSessionTransport(emitter, sessionId);
        McpServerSession session = sessionFactory.create(sessionTransport);
        sessions.put(sessionId, session);

        emitter.onCompletion(() -> sessions.remove(sessionId));
        emitter.onTimeout(() -> sessions.remove(sessionId));

        new Thread(() -> {
            try {
                // 커스텀 클라이언트는 endpoint 이벤트를 무시할 수 있지만, 표준 호환성을 위해 전송
                Thread.sleep(100);
                emitter.send(SseEmitter.event().name("endpoint").data(messageEndpoint + "?sessionId=" + sessionId));

                // Body로 들어온 initialize 등 즉시 처리
                if (body != null && !body.trim().isEmpty()) {
                    handleMessage(sessionId, body);
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    public ResponseEntity<String> handleMessage(String sessionId, String body) {
        log.info("Received POST message for sessionId: " + sessionId + ", body: " + body);
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            return ResponseEntity.badRequest().body("Missing or invalid sessionId");
        }

        McpServerSession session = sessions.get(sessionId);
        try {
            Map<String, Object> map = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            JSONRPCMessage message;

            if (map.containsKey("id")) {
                if (map.containsKey("method")) {
                    message = objectMapper.convertValue(map, JSONRPCRequest.class);
                } else {
                    message = objectMapper.convertValue(map, JSONRPCResponse.class);
                }
            } else {
                message = objectMapper.convertValue(map, JSONRPCNotification.class);
            }
            log.info("Converted message type: " + message.getClass().getName());

            session.handle(message).subscribe();
            log.info("Message sent to session handler");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to handle message", e);
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    private class CustomMcpSessionTransport implements McpServerTransport {
        private final SseEmitter emitter;
        private final String sessionId;

        public CustomMcpSessionTransport(SseEmitter emitter, String sessionId) {
            this.emitter = emitter;
            this.sessionId = sessionId;
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return Mono.fromRunnable(() -> {
                log.info("Sending message to SSE stream: " + message.getClass().getName());
                try {
                    String json = objectMapper.writeValueAsString(message);
                    log.info("Serialized message: " + json);
                    emitter.send(SseEmitter.event().name("message").data(json));
                    log.info("Message successfully sent to SSE emitter");
                } catch (Exception e) {
                    log.error("Error sending message to SSE emitter", e);
                    emitter.completeWithError(e);
                }
            });
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(emitter::complete);
        }

        @Override
        public <T> T unmarshalFrom(Object object, TypeRef<T> typeRef) {
            return objectMapper.convertValue(object, objectMapper.constructType(typeRef.getType()));
        }
    }
}
