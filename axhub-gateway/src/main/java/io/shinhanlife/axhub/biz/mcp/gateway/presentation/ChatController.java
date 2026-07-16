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
    public SseEmitter chatStream(@RequestBody Map<String, String> request,
                                 @RequestHeader(value = "X-Agent-Id", required = false) String agentId,
                                 @RequestHeader(value = "X-Tenant-Id", required = false, defaultValue = "system") String tenantId) {
        String effectiveTenantId = (agentId != null && !agentId.trim().isEmpty()) ? agentId : tenantId;
        String message = request.getOrDefault("message", "").trim();
        log.info("[Real AI Chat] 사용자의 메시지 수신: {}", message);

        SseEmitter emitter = new SseEmitter(120000L); // 120 seconds timeout

        try {
            List<ToolCallback> callbacks = new ArrayList<>();
            for (ToolMetadata meta : registryService.getAllTools()) {
                if (Boolean.TRUE.equals(meta.getVisible())) {
                    callbacks.add(new DynamicMcpToolCallback(meta, executeService, objectMapper, effectiveTenantId));
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
                            String errorMsg = error.getMessage();
                            if (errorMsg != null && (errorMsg.contains("timeout") || errorMsg.contains("OpenAIIoException"))) {
                                emitter.send("\n\n⚠️ **요청 시간이 초과되었습니다.** (현재 여러 개의 도구를 분석하느라 모델의 응답이 지연되었습니다. 해당하는 도구가 없거나 너무 복잡한 요청일 수 있습니다.)");
                            } else if (errorMsg != null && errorMsg.contains("503")) {
                                emitter.send("\n\n⚠️ **AI 모델 서버 혼잡 (503)**: 현재 AI 모델을 제공하는 서버에 일시적으로 접속자가 많아 지연이 발생하고 있습니다. 잠시 후 다시 시도해 주세요.");
                            } else if (errorMsg != null && errorMsg.contains("429")) {
                                emitter.send("\n\n⚠️ **API 사용량 초과 (429)**: 현재 사용 중인 Gemini API(무료 티어)의 일일 또는 분당 요청 한도를 초과했습니다. 잠시 후 다시 시도하시거나 API 플랜을 확인해 주세요.");
                            } else {
                                emitter.send("\n[에러 발생: " + errorMsg + "]");
                            }
                        } catch (Exception ignore) {}
                        emitter.complete(); // 클라이언트 측에서 네트워크 에러로 처리하지 않도록 정상 종료
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
        private final String tenantId;

        public DynamicMcpToolCallback(ToolMetadata metadata, ExecuteService executeService, ObjectMapper objectMapper, String tenantId) {
            this.metadata = metadata;
            this.executeService = executeService;
            this.objectMapper = objectMapper;
            this.tenantId = tenantId;
            
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

                Object result = executeService.execute(payload, this.tenantId);
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
