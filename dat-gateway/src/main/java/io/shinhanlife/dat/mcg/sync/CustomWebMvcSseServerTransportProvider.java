package io.shinhanlife.dat.mcg.sync;

/**
 * @package io.shinhanlife.dat.mcg.sync
 * @className CustomWebMvcSseServerTransportProvider
 * @description AX HUB MCP Gateway SSE 전송 제공자 - SSE 기반의 MCP 서버 트랜스포트를 구현하는 클래스
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
    private final Map<String, CustomMcpSessionTransport> customTransports = new ConcurrentHashMap<>();
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
            customTransports.clear();
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

        // emitter.onCompletion(() -> sessions.remove(sessionId));
        // emitter.onTimeout(() -> sessions.remove(sessionId));

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

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter handleCustomSse(String sessionId, String body) {
        if (sessionFactory == null) {
            throw new IllegalStateException("SessionFactory not configured");
        }
        
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(-1L);
        
        boolean isNew = !sessions.containsKey(sessionId);
        
        if (isNew) {
            CustomMcpSessionTransport sessionTransport = new CustomMcpSessionTransport(emitter, sessionId);
            customTransports.put(sessionId, sessionTransport);
            McpServerSession session = sessionFactory.create(sessionTransport);
            sessions.put(sessionId, session);
            
            // 주의: 클라이언트가 단일 POST 응답 후 연결을 끊더라도, 
            // 웜 풀(Warm Pool) 스펙상 세션은 살려둬야 하므로 세션 삭제 로직 제외
        } else {
            // 기존 세션인 경우 Emitter 파이프만 덮어씌움 (Switching)
            CustomMcpSessionTransport sessionTransport = customTransports.get(sessionId);
            if (sessionTransport != null) {
                sessionTransport.setEmitter(emitter);
            }
        }
        
        new Thread(() -> {
            try {
                // Body로 들어온 메시지 즉시 비동기 처리
                if (body != null && !body.trim().isEmpty()) {
                    handleMessage(sessionId, body, emitter);
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }

    public org.springframework.http.ResponseEntity<String> handleMessage(String sessionId, String body) {
        return handleMessage(sessionId, body, null);
    }

    public org.springframework.http.ResponseEntity<String> handleMessage(String sessionId, String body, org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter) {
        log.info("Received POST message for sessionId: " + sessionId + ", body: " + body);
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            return ResponseEntity.badRequest().body("Unexpected request body");
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
            
            if (emitter != null && !map.containsKey("id")) {
                // emitter.complete();
                log.info("Completed emitter for notification (disabled for keep-alive)");
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to handle message", e);
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    public boolean hasSession(String sessionId) {
        return sessionId != null && sessions.containsKey(sessionId);
    }

    private class CustomMcpSessionTransport implements McpServerTransport {
        private volatile org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter;
        private final String sessionId;

        public CustomMcpSessionTransport(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter, String sessionId) {
            this.emitter = emitter;
            this.sessionId = sessionId;
        }

        public void setEmitter(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return Mono.fromRunnable(() -> {
                log.debug("Sending message to SSE stream: {}", message.getClass().getName());
                try {
                    String json = objectMapper.writeValueAsString(message);
                    log.debug("Serialized message: {}", json);
                    if (this.emitter != null) {
                        this.emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("message").data(json));
                        log.debug("Message successfully sent to SSE emitter");
                        
                        // Custom 프로토콜: 1회 요청당 1응답 후 종료 (스트림을 닫아버림)
                        // 클라이언트가 한 번의 POST 후 응답을 받고 연결을 끊기 때문
                        // this.emitter.complete(); // MCP 표준 클라이언트 지원을 위해 스트림 강제 종료 제거
                    }
                } catch (Exception e) {
                    if (e.getClass().getSimpleName().contains("AsyncRequestNotUsableException") || 
                        (e.getCause() != null && e.getCause() instanceof java.io.IOException) ||
                        (e instanceof IllegalStateException && e.getMessage() != null && e.getMessage().contains("already completed"))) {
                        log.debug("SSE emitter is disconnected (expected in Warm Pool): {}", e.getMessage());
                    } else {
                        log.error("Error sending message to SSE emitter", e);
                    }
                    
                    if (this.emitter != null) {
                        try {
                            this.emitter.completeWithError(e);
                        } catch (Exception ignored) {
                            // ignore already completed
                        } finally {
                            // 죽은 Emitter를 null로 초기화하여 다음 5초 주기 때 재시도하지 않도록 방지
                            // 클라이언트가 재연결하면 setEmitter(새 Emitter)를 통해 덮어씌워짐
                            this.emitter = null;
                        }
                    }
                }
            });
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(() -> {
                if (this.emitter != null) {
                    this.emitter.complete();
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object object, TypeRef<T> typeRef) {
            return objectMapper.convertValue(object, objectMapper.constructType(typeRef.getType()));
        }
    }
}
