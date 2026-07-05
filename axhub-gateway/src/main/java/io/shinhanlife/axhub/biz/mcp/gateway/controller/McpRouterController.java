package io.shinhanlife.axhub.biz.mcp.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.axhub.biz.mcp.adapter.dto.JsonRpcRequest;
import io.shinhanlife.axhub.biz.mcp.adapter.dto.JsonRpcResponse;
import io.shinhanlife.axhub.biz.mcp.adapter.dto.Params;
import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.gateway.service.ExecuteService;
import io.shinhanlife.axhub.biz.mcp.gateway.registry.RedisRegistryService;
import io.shinhanlife.axhub.common.mcp.security.SecurityProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/mcp/api/v1")
@Tag(name = "MCP Router API", description = "AI Agent의 요청을 받아 Adapter 시스템으로 라우팅하는 게이트웨이 API")
public class McpRouterController {

    private final RedisRegistryService redisRegistryService;
    private final ExecuteService executeService;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public McpRouterController(RedisRegistryService redisRegistryService,
                               ExecuteService executeService,
                               SecurityProperties securityProperties,
                               ObjectMapper objectMapper) {
        this.redisRegistryService = redisRegistryService;
        this.executeService = executeService;
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
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
            Object result = executeService.execute(payload, tenantId);
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
        List<ToolMetadata> activeTools = redisRegistryService.getAllTools();
        
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(UUID.randomUUID().toString());
        response.setResult(Map.of("tools", activeTools));
        
        return ResponseEntity.ok(response);
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
