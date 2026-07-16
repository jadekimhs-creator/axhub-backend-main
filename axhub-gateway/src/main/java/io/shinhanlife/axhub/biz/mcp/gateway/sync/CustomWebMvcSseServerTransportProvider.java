package io.shinhanlife.axhub.biz.mcp.gateway.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RequestPredicates.accept;

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

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter handleSse() {
        if (sessionFactory == null) {
            throw new IllegalStateException("SessionFactory not configured");
        }
        
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(-1L);
        String sessionId = UUID.randomUUID().toString();
        
        CustomMcpSessionTransport sessionTransport = new CustomMcpSessionTransport(emitter, sessionId);
        McpServerSession session = sessionFactory.create(sessionTransport);
        sessions.put(sessionId, session);
        
        emitter.onCompletion(() -> sessions.remove(sessionId));
        emitter.onTimeout(() -> sessions.remove(sessionId));
        
        new Thread(() -> {
            try {
                Thread.sleep(100);
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("endpoint").data(messageEndpoint + "?sessionId=" + sessionId));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }

    public org.springframework.http.ResponseEntity<String> handleMessage(String sessionId, String body) {
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            return org.springframework.http.ResponseEntity.badRequest().body("Missing or invalid sessionId");
        }
        
        McpServerSession session = sessions.get(sessionId);
        try {
            java.util.Map<String, Object> map = objectMapper.readValue(body, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
            io.modelcontextprotocol.spec.McpSchema.JSONRPCMessage message;
            
            if (map.containsKey("id")) {
                if (map.containsKey("method")) {
                    message = objectMapper.convertValue(map, io.modelcontextprotocol.spec.McpSchema.JSONRPCRequest.class);
                } else {
                    message = objectMapper.convertValue(map, io.modelcontextprotocol.spec.McpSchema.JSONRPCResponse.class);
                }
            } else {
                message = objectMapper.convertValue(map, io.modelcontextprotocol.spec.McpSchema.JSONRPCNotification.class);
            }
            
            session.handle(message).subscribe();
            return org.springframework.http.ResponseEntity.ok().build();
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).body(e.getMessage());
        }
    }

    private class CustomMcpSessionTransport implements McpServerTransport {
        private final org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter;
        private final String sessionId;

        public CustomMcpSessionTransport(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter, String sessionId) {
            this.emitter = emitter;
            this.sessionId = sessionId;
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return Mono.fromRunnable(() -> {
                try {
                    String json = objectMapper.writeValueAsString(message);
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("message").data(json));
                } catch (Exception e) {
                    throw new RuntimeException(e);
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
