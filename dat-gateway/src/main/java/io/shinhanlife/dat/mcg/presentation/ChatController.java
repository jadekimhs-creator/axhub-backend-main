package io.shinhanlife.dat.mcg.presentation;


/**
 * @package io.shinhanlife.dat.mcg.presentation
 * @className ChatController
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
import io.shinhanlife.dat.lib.adapter.dto.JsonRpcResponse;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import io.shinhanlife.dat.mcg.registry.InMemoryRegistryService;
import io.shinhanlife.dat.mcg.service.ExecuteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final InMemoryRegistryService registryService;
    private final ChatClient.Builder chatClientBuilder;
    private final McpRouterController mcpRouterController;

    @org.springframework.beans.factory.annotation.Value("${shinhan.ai.base-url}")
    private String liteLlmBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${shinhan.ai.gemma-key}")
    private String gemmaKey;

    @org.springframework.beans.factory.annotation.Value("${shinhan.ai.qwen-key}")
    private String qwenKey;

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
            JsonRpcResponse toolsResponse = mcpRouterController.listTools(null).getBody();
            if (toolsResponse != null && toolsResponse.getResult() instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) toolsResponse.getResult();
                if (resultMap.containsKey("tools")) {
                    List<Map<String, Object>> allTools = (List<Map<String, Object>>) resultMap.get("tools");
                    Set<String> addedToolNames = new HashSet<>();
                    for (Map<String, Object> metaMap : allTools) {
                        String name = (String) metaMap.get("name");
                        if (name != null) {
                            String cleanName = name.replaceAll("[^a-zA-Z0-9_-]", "_");
                            if (!addedToolNames.contains(cleanName)) {
                                ToolMetadata meta = ToolMetadata.builder()
                                        .name(name)
                                        .description((String) metaMap.get("description"))
                                        .parametersSchema((Map<String, Object>) metaMap.get("inputSchema"))
                                        .build();
                                callbacks.add(new DynamicMcpToolCallback(meta, executeService, objectMapper, effectiveTenantId));
                                addedToolNames.add(cleanName);
                            }
                        }
                    }
                }
            }

            String selectedModel = request.getOrDefault("model", "Qwen3-Coder").trim();
            if (selectedModel.isEmpty()) {
                selectedModel = "Qwen3-Coder";
            }

            // LiteLLM 내부망 모델 매핑
            if (selectedModel.equals("gemini-flash-latest")) {
                selectedModel = "Gemma-4-31B";
                log.info("[Real AI Chat] 화면의 Gemini 플래시 선택을 사내 Gemma-4-31B 모델로 라우팅 전환");
            }

            ChatClient activeChatClient;
            org.springframework.ai.chat.prompt.ChatOptions chatOptions;

            // 2. 모델 분기 처리: 사내 모델(Qwen3/Gemma-4) vs 기존 외부 모델(OpenRouter 등)
            if ("Qwen3-Coder".equalsIgnoreCase(selectedModel) || "Gemma-4-31B".equalsIgnoreCase(selectedModel)) {
                // 사내 LiteLLM 환경 동적 ChatModel 생성
                // Use liteLlmBaseUrl from application.yml
                String apiKey = "Gemma-4-31B".equalsIgnoreCase(selectedModel) ? gemmaKey : qwenKey;
                
                org.springframework.ai.openai.api.OpenAiApi openAiApi = org.springframework.ai.openai.api.OpenAiApi.builder()
                        .baseUrl(liteLlmBaseUrl)
                        .apiKey(new org.springframework.ai.model.SimpleApiKey(apiKey))
                        .build();
                        
                org.springframework.ai.openai.OpenAiChatModel dynamicChatModel = org.springframework.ai.openai.OpenAiChatModel.builder()
                        .openAiApi(openAiApi)
                        .build();

                activeChatClient = ChatClient.builder(dynamicChatModel)
                        .defaultSystem("You are AX HUB Assistant, a highly capable enterprise AI agent. You must use the provided tools to answer user questions when necessary. Always answer politely in Korean.")
                        .build();
                
                chatOptions = org.springframework.ai.openai.OpenAiChatOptions.builder()
                        .model(selectedModel)
                        .temperature(0.2)
                        .maxTokens(16384)
                        .build();
            } else {
                // 기존 방식: application.yml 에 설정된 기본 Bean (OpenRouter 등) 사용
                activeChatClient = chatClientBuilder
                        .defaultSystem("You are AX HUB Assistant, a highly capable enterprise AI agent. You must use the provided tools to answer user questions when necessary. Always answer politely in Korean.")
                        .build();
                
                chatOptions = org.springframework.ai.openai.OpenAiChatOptions.builder()
                        .model(selectedModel)
                        .build();
            }

            Flux<String> responseStream = activeChatClient.prompt()
                    .user(message)
                    .toolCallbacks(callbacks.toArray(new ToolCallback[0]))
                    .options(chatOptions)
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
                                emitter.send("\n\n⚠️ **API 사용량 초과 (429)**: AI 모델 API(무료 티어 또는 오픈라우터)의 요청 한도를 초과했습니다. 잠시 후 다시 시도하시거나 API 키의 유효 한도를 확인해 주세요.");
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
