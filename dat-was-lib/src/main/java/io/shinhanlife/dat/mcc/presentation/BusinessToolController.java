package io.shinhanlife.dat.mcc.presentation;

import io.shinhanlife.dat.lib.mcp.McpRequestHeaders;
import io.shinhanlife.dat.lib.mcp.ToolExecutionResult;
import io.shinhanlife.dat.lib.mcp.McpToolExecutionService;
import io.shinhanlife.dat.lib.mcp.ToolRegistryHeartbeatSender;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Legacy REST adapter for Tool Pod execution. */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class BusinessToolController {

    private final ToolRegistryHeartbeatSender toolRegistryHeartbeatSender;
    private final McpToolExecutionService toolExecutionService;

    @GetMapping("/mcp/api/v1/tools/local")
    public List<ToolMetadata> getLocalTools() {
        return toolRegistryHeartbeatSender.getAllScannedTools();
    }

    @PostMapping("/mcp/{name}")
    public ResponseEntity<?> executeDynamicTool(
            @PathVariable("name") String functionName,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestHeader(value = "guid", required = false) String guid,
            @RequestHeader(value = "mcp-session-id", required = false) String mcpSessionId,
            @RequestHeader(value = "employee-no", required = false) String employeeNo,
            @RequestHeader(value = "virtual-employee-no", required = false) String virtualEmployeeNo,
            @RequestBody(required = false) Map<String, Object> arguments) {
        ToolExecutionResult result = toolExecutionService.execute(
                functionName,
                new McpRequestHeaders(requestId, guid, mcpSessionId, employeeNo, virtualEmployeeNo),
                arguments);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(result.statusCode());
        result.headers().forEach(response::header);
        return response.body(result.body());
    }
}
