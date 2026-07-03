package io.shinhanlife.axhub.biz.mcp.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.axhub.biz.mcp.adapter.connector.LegacyEimsConnector;
import io.shinhanlife.axhub.biz.mcp.adapter.connector.ThirdPartySecurityConnector;
import io.shinhanlife.axhub.common.mcp.adapter.dto.JsonRpcRequest;
import io.shinhanlife.axhub.common.mcp.adapter.dto.JsonRpcResponse;
import io.shinhanlife.axhub.common.mcp.adapter.dto.Params;
import io.shinhanlife.axhub.biz.mcp.adapter.util.PiiMaskingUtils;
import io.shinhanlife.axhub.common.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.gateway.service.ExecuteService;
import io.shinhanlife.axhub.biz.mcp.gateway.registry.RedisRegistryService;
import io.shinhanlife.axhub.common.mcp.security.SecurityProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;

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
    private final LegacyEimsConnector legacyEimsConnector;
    private final ThirdPartySecurityConnector thirdPartySecurityConnector;
    private final ObjectMapper objectMapper;

    // 생성자 주입
    public McpRouterController(RedisRegistryService redisRegistryService,
                               ExecuteService executeService,
                               SecurityProperties securityProperties,
                               LegacyEimsConnector legacyEimsConnector,
                               ThirdPartySecurityConnector thirdPartySecurityConnector,
                               ObjectMapper objectMapper) {
        this.redisRegistryService = redisRegistryService;
        this.executeService = executeService;
        this.securityProperties = securityProperties;
        this.legacyEimsConnector = legacyEimsConnector;
        this.thirdPartySecurityConnector = thirdPartySecurityConnector;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Adapter 툴 실행 라우팅 (로컬 모듈화)", description = "AI Agent의 요청을 받아 직접 레거시 시스템을 호출합니다.")
    @PostMapping("/execute-tool")
    public ResponseEntity<Map<String, Object>> routeToPreBuildTool(@RequestBody Map<String, Object> agentPayload) {
        String trackId = UUID.randomUUID().toString();
        log.info("[{}]  AI Agent로부터 툴 실행 요청 수신 완료", trackId);
        log.info("[{}]  AI Agent가 보낸 원본 payload: {}", trackId, agentPayload);

        try {
            // 1. 요청 파싱 (Map -> JsonRpcRequest)
            agentPayload.put("id", trackId);
            JsonRpcRequest request = objectMapper.convertValue(agentPayload, JsonRpcRequest.class);
            Params params = request.getParams();

            if (params == null) {
                throw new IllegalArgumentException("Invalid params: params 객체가 비어있습니다.");
            }

            String routingType = params.getRoutingType();
            String interfaceId = params.getInterfaceId();

            // 2. 라우터 실행 (분기 처리) - HTTP 통신 없이 직접 호출!
            String executionResult;
            if (interfaceId != null && (interfaceId.startsWith("DRM_") || interfaceId.startsWith("BM_"))) {
                executionResult = thirdPartySecurityConnector.executeSecurityModule(interfaceId, params.getData());
            } else {
                executionResult = legacyEimsConnector.executeByTool(routingType, interfaceId, params.getData(), params.getSpec());
            }

            // 3. PII 마스킹
            String maskedResult = PiiMaskingUtils.mask(executionResult);

            // 4. 응답 조립 (JsonRpcResponse -> Map)
            JsonRpcResponse response = new JsonRpcResponse();
            response.setId(trackId);
            response.setResult(maskedResult);
            
            Map<String, Object> rpcResponse = objectMapper.convertValue(response, Map.class);
            log.info("[{}]  로컬 Adapter 처리 성공", trackId);
            
            return ResponseEntity.ok(rpcResponse);

        } catch (ResourceAccessException e) {
            log.error("[{}]  Adapter 시스템 응답 시간 초과 (Timeout) 발생! 사유: {}", trackId, e.getMessage());
            return ResponseEntity.status(504).body(Map.of(
                    "jsonrpc", "2.0",
                    "error", Map.of("code", -32603, "message", "인터페이스 서버 연결 시간 초과 (Timeout)"),
                    "id", trackId
            ));

        } catch (Exception e) {
            log.error("[{}]  시스템 예외 에러 발생: {}", trackId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "jsonrpc", "2.0",
                    "error", Map.of("code", -32000, "message", "내부 게이트웨이 서버 오류: " + e.getMessage()),
                    "id", trackId
            ));
        }
    }

    // 1. [신규] 표준 MCP 파이프라인 호출 (가장 중요!)
    @Operation(summary = "MCP 파이프라인 호출", description = "MCP 표준 파이프라인(ExecuteService)을 통해 레거시 툴을 호출합니다.")
    @PostMapping("/tools/call")
    public ResponseEntity<Object> callTool(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Agent-Id", required = false) String agentId,
            @RequestHeader(value = "X-User-Prompt", required = false) String userPrompt,
            @RequestBody Map<String, Object> payload, 
            @RequestAttribute(value = "tenantId", required = false) String tenantId) {
            
        // 메타데이터 주입
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("traceId", traceId != null ? traceId : java.util.UUID.randomUUID().toString());
        meta.put("agentId", agentId != null ? agentId : "UNKNOWN");
        meta.put("userPrompt", userPrompt != null ? userPrompt : "");
        payload.put("meta", meta);
        
        log.info("[MCP 표준] ExecuteService 파이프라인을 통한 툴 호출 시작 (Tenant: {}, Trace: {})", tenantId, meta.get("traceId"));
        if (payload != null) {
            try {
                log.info(" [Gateway] AI Agent 요청 파라미터: {}", objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                log.info(" [Gateway] AI Agent 요청 파라미터: {}", payload);
            }
        }

        try {
            Object result = executeService.execute(payload, tenantId);
            try {
                log.info("\n [MCP Gateway -> AI Agent] 최종 응답 반환: {}", objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                log.info("\n [MCP Gateway -> AI Agent] 최종 응답 반환: {}", result);
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

    // 2. 기존 표준 규격 메서드들 (유지 및 동적 Registry 반영)
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
