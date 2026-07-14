package io.shinhanlife.axhub.biz.mcp.gateway.presentation;

import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.gateway.registry.RedisRegistryService;
import io.shinhanlife.axhub.biz.mcp.gateway.service.ExecuteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ExecuteService executeService;
    private final ObjectMapper objectMapper;
    private final RedisRegistryService registryService;
    private final ChatClient.Builder chatClientBuilder;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "").trim();
        log.info("[Real AI Chat] 사용자의 메시지 수신: {}", message);

        SseEmitter emitter = new SseEmitter(120000L); // 120 seconds timeout

        try {
            List<ToolCallback> callbacks = new ArrayList<>();
            for (ToolMetadata meta : registryService.getAllTools()) {
                if (Boolean.TRUE.equals(meta.getVisible())) {
                    callbacks.add(new DynamicMcpToolCallback(meta, executeService, objectMapper));
                }
            }

            ChatClient chatClient = chatClientBuilder
                    .defaultSystem("You are AX HUB Assistant, a highly capable enterprise AI agent. You must use the provided tools to answer user questions when necessary. Always answer politely in Korean.")
                    .build();

            Flux<String> responseStream = chatClient.prompt()
                    .user(message)
                    .tools((Object[]) callbacks.toArray(new ToolCallback[0])) // Spring AI 2.0 uses tools()
                    .stream()
                    .content();

            responseStream.subscribe(
                    chunk -> {
                        try {
                            if (chunk != null) {
                                emitter.send(chunk);
                            }
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("[Real AI Chat] 스트리밍 중 에러 발생", error);
                        try {
                            emitter.send("\n[에러 발생: " + error.getMessage() + "]");
                        } catch (Exception ignore) {}
                        emitter.completeWithError(error);
                    },
                    () -> {
                        log.info("[Real AI Chat] 스트리밍 완료");
                        emitter.complete();
                    }
            );

        } catch (Exception e) {
            log.error("[Real AI Chat] 초기화 중 에러 발생", e);
            try {
                emitter.send("초기화 중 에러가 발생했습니다: " + e.getMessage());
            } catch (Exception ignore) {}
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private static class DynamicMcpToolCallback implements ToolCallback {
        private final ToolMetadata metadata;
        private final ExecuteService executeService;
        private final ObjectMapper objectMapper;
        private final ToolDefinition toolDefinition;

        public DynamicMcpToolCallback(ToolMetadata metadata, ExecuteService executeService, ObjectMapper objectMapper) {
            this.metadata = metadata;
            this.executeService = executeService;
            this.objectMapper = objectMapper;
            
            String schema = "{\"type\":\"object\",\"properties\":{}}";
            try {
                if (metadata.getParametersSchema() != null) {
                    schema = objectMapper.writeValueAsString(metadata.getParametersSchema());
                }
            } catch (Exception e) {
                log.warn("Schema parsing error for tool: {}", metadata.getName());
            }
            
            this.toolDefinition = ToolDefinition.builder()
                .name(metadata.getName().replaceAll("[^a-zA-Z0-9_-]", "_"))
                .description(metadata.getDescription() != null ? metadata.getDescription() : "No description provided")
                .inputSchema(schema)
                .build();
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return this.toolDefinition;
        }

        @Override
        public String call(String toolInput) {
            log.info("[Function Calling] LLM이 '{}' 툴을 호출했습니다. 입력값: {}", metadata.getName(), toolInput);
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("jsonrpc", "2.0");
                payload.put("method", "tools/call");
                payload.put("id", UUID.randomUUID().toString());

                Map<String, Object> params = new HashMap<>();
                params.put("name", metadata.getName());
                
                if (toolInput != null && !toolInput.trim().isEmpty()) {
                    Map<String, Object> arguments = objectMapper.readValue(toolInput, Map.class);
                    params.put("arguments", arguments);
                } else {
                    params.put("arguments", new HashMap<>());
                }
                
                payload.put("params", params);

                Object result = executeService.execute(payload, "default");
                String jsonResult = objectMapper.writeValueAsString(result);
                log.info("[Function Calling] '{}' 툴 실행 완료. 결과: {}", metadata.getName(), jsonResult);
                return jsonResult;
            } catch (Exception e) {
                log.error("[Function Calling] '{}' 툴 실행 중 에러", metadata.getName(), e);
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }
    }
}
