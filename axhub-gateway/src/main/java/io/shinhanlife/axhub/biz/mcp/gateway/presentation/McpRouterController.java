package io.shinhanlife.axhub.biz.mcp.gateway.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.axhub.biz.mcp.adapter.dto.JsonRpcRequest;
import io.shinhanlife.axhub.biz.mcp.adapter.dto.JsonRpcResponse;
import io.shinhanlife.axhub.biz.mcp.adapter.dto.Params;
import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.gateway.service.ExecuteService;
import io.shinhanlife.axhub.biz.mcp.gateway.config.GatewayFallbackProperties;
import io.shinhanlife.axhub.biz.mcp.gateway.registry.RedisRegistryService;
import io.shinhanlife.axhub.common.mcp.security.SecurityProperties;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @package io.shinhanlife.axhub.biz.mcp.gateway.presentation
 * @className McpRouterController
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
@RestController
@RequestMapping("/mcp/api/v1")
@Tag(name = "MCP Router API", description = "AI Agent의 요청을 받아 Adapter 시스템으로 라우팅하는 게이트웨이 API")
public class McpRouterController {

    private final RedisRegistryService redisRegistryService;
    private final ExecuteService executeService;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;
    private final GatewayFallbackProperties gatewayFallbackProperties;
    private final RestClient restClient;

    public McpRouterController(RedisRegistryService redisRegistryService,
                               ExecuteService executeService,
                               SecurityProperties securityProperties,
                               ObjectMapper objectMapper,
                               GatewayFallbackProperties gatewayFallbackProperties) {
        this.redisRegistryService = redisRegistryService;
        this.executeService = executeService;
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
        this.gatewayFallbackProperties = gatewayFallbackProperties;
        this.restClient = RestClient.create();
    }

    @Operation(summary = "MCP 파이프라인 호출", description = "MCP 표준 파이프라인(ExecuteService)을 통해 레거시 툴을 호출합니다.")
    @PostMapping("/tools/call")
    public ResponseEntity<Object> callTool(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Agent-Id", required = false) String agentId,
            @RequestHeader(value = "X-User-Prompt", required = false) String userPrompt,
            @RequestBody Map<String, Object> payload, 
            @RequestAttribute(value = "tenantId", required = false) String tenantId) {
            
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("traceId", traceId != null ? traceId : java.util.UUID.randomUUID().toString());
        meta.put("agentId", agentId != null ? agentId : "UNKNOWN");
        meta.put("userPrompt", userPrompt != null ? userPrompt : "");
        payload.put("meta", meta);
        
        log.info("[MCP 표준] ExecuteService 파이프라인을 통한 툴 호출 시작 (Tenant: {}, Trace: {})", tenantId, meta.get("traceId"));
        
        try {
            log.info("[AI -> MCP Gateway] 동적 툴 실행 요청 수신: {}", objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            log.info("[AI -> MCP Gateway] 동적 툴 실행 요청 수신: {}", payload);
        }

        try {
            Object result = executeService.execute(payload, tenantId);
            
            try {
                log.info("[MCP Gateway -> AI] 동적 툴 실행 결과 반환: {}", objectMapper.writeValueAsString(result));
            } catch (Exception ex) {
                log.info("[MCP Gateway -> AI] 동적 툴 실행 결과 반환: {}", result);
            }
            
            return ResponseEntity.ok(result);
        } catch (SecurityException se) {
            log.warn(" [보안 차단] 권한 오류: {}", se.getMessage());
            return ResponseEntity.status(403).body(Map.of("error", se.getMessage()));
        } catch (Exception e) {
            log.error(" 파이프라인 실행 중 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tools/list")
    public ResponseEntity<JsonRpcResponse> listTools() {
        List<ToolMetadata> activeTools = redisRegistryService.getAllTools()
                .stream()
                .filter(ToolMetadata::isVisible)
                .collect(java.util.stream.Collectors.toList());
                
        java.util.Set<String> knownTools = activeTools.stream()
                .map(ToolMetadata::getToolName)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<String> fallbackUrls = new java.util.HashSet<>(gatewayFallbackProperties.getRoutes().values());
        if (gatewayFallbackProperties.getDefaultUrl() != null) {
            fallbackUrls.add(gatewayFallbackProperties.getDefaultUrl());
        }

        for (String url : fallbackUrls) {
            try {
                List<ToolMetadata> localTools = restClient.get()
                        .uri(url + "/mcp/api/v1/tools/local")
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<ToolMetadata>>() {});
                        
                if (localTools != null) {
                    for (ToolMetadata t : localTools) {
                        if (!Boolean.TRUE.equals(t.getIsRegistered()) && Boolean.TRUE.equals(t.getVisible()) && !knownTools.contains(t.getToolName())) {
                            activeTools.add(t);
                            knownTools.add(t.getToolName());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch local tools from fallback URL: {}", url);
            }
        }
        
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(UUID.randomUUID().toString());
        response.setResult(Map.of("tools", activeTools));
        
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/tools/docs/markdown", produces = "text/markdown;charset=UTF-8")
    public ResponseEntity<String> generateToolsMarkdown() {
        List<ToolMetadata> tools = redisRegistryService.getAllTools()
                .stream()
                .filter(ToolMetadata::isVisible)
                .collect(java.util.stream.Collectors.toList());
        
        StringBuilder md = new StringBuilder();
        md.append("# \uD83E\uDD16 Shinhan AI Tool Catalog\n\n");
        md.append("**총 등록된 툴:** ").append(tools.size()).append("개\n");
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        md.append("**마지막 업데이트:** ").append(java.time.LocalDateTime.now().format(formatter)).append("\n\n");
        md.append("---\n\n");
        
        // Group by Domain Group
        Map<String, List<ToolMetadata>> groupedTools = new java.util.HashMap<>();
        for (ToolMetadata tool : tools) {
            String group = tool.getDomainGroup() != null ? tool.getDomainGroup() : "기타 (Others)";
            groupedTools.computeIfAbsent(group, k -> new java.util.ArrayList<>()).add(tool);
        }
        
        for (Map.Entry<String, List<ToolMetadata>> entry : groupedTools.entrySet()) {
            md.append("## \uD83D\uDCC1 도메인: ").append(entry.getKey()).append("\n\n");
            
            int index = 1;
            for (ToolMetadata tool : entry.getValue()) {
                md.append("### ").append(index++).append(". ").append(tool.getToolName()).append("\n");
                if (tool.getDescription() != null) {
                    md.append("- **설명**: ").append(tool.getDescription()).append("\n");
                }
                md.append("- **연동 방식**: `").append(tool.getIntegrationType() != null ? tool.getIntegrationType() : "DIRECT").append("`\n\n");
                
                // Parameters Table
                if (tool.getParametersSchema() != null && tool.getParametersSchema().containsKey("properties")) {
                    md.append("#### \u2699\uFE0F 파라미터 (Parameters)\n");
                    md.append("| 파라미터명 | 타입 | 필수 여부 | 설명 |\n");
                    md.append("|---|---|---|---|\n");
                    
                    Map<String, Object> properties = (Map<String, Object>) tool.getParametersSchema().get("properties");
                    List<String> required = (List<String>) tool.getParametersSchema().get("required");
                    
                    for (Map.Entry<String, Object> prop : properties.entrySet()) {
                        String name = prop.getKey();
                        Map<String, Object> details = (Map<String, Object>) prop.getValue();
                        String type = details.containsKey("type") ? String.valueOf(details.get("type")) : "string";
                        String desc = details.containsKey("description") ? String.valueOf(details.get("description")) : "";
                        String req = (required != null && required.contains(name)) ? "Y" : "N";
                        
                        md.append("| `").append(name).append("` | `").append(type).append("` | ").append(req).append(" | ").append(desc).append(" |\n");
                    }
                    md.append("\n");
                }
                
                // Action Prompts
                if (tool.getActionPrompts() != null && !tool.getActionPrompts().isEmpty()) {
                    md.append("#### \uD83D\uDCAC 프롬프트 예시 (Action Prompts)\n");
                    for (Map.Entry<String, String> prompt : tool.getActionPrompts().entrySet()) {
                        md.append("- \"").append(prompt.getValue()).append("\"\n");
                    }
                    md.append("\n");
                }
                md.append("---\n\n");
            }
        }
        
        return ResponseEntity.ok(md.toString());
    }

    @PostMapping("/registry/register")
    public ResponseEntity<String> registerTool(@RequestBody ToolMetadata meta) {
        redisRegistryService.saveTool(meta);
        return ResponseEntity.ok("Registered");
    }

    @PostMapping("/registry/deregister")
    public ResponseEntity<String> deregisterTool(@RequestBody String toolName) {
        redisRegistryService.removeTool(toolName);
        return ResponseEntity.ok("Deregistered");
    }

    @PostMapping("/registry/heartbeat")
    public ResponseEntity<String> heartbeat(@RequestBody String toolName) {
        boolean success = redisRegistryService.refreshHeartbeat(toolName);
        if (success) {
            return ResponseEntity.ok("Heartbeat updated");
        } else {
            return ResponseEntity.status(404).body("Tool not found");
        }
    }
}