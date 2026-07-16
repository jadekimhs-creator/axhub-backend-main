package io.shinhanlife.axhub.biz.mcp.gateway.sync;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class DynamicMcpController {

    private final DynamicMcpServerManager manager;

    public DynamicMcpController(DynamicMcpServerManager manager) {
        this.manager = manager;
    }

    @GetMapping("/mcp/sse/{category}")
    public SseEmitter handleSse(@PathVariable("category") String category) {
        CustomWebMvcSseServerTransportProvider transport = manager.getTransport(category);
        if (transport == null) {
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        return transport.handleSse();
    }

    @PostMapping("/mcp/message/{category}")
    public ResponseEntity<String> handleMessage(
            @PathVariable("category") String category,
            @RequestParam("sessionId") String sessionId,
            @RequestBody String body) {
            
        CustomWebMvcSseServerTransportProvider transport = manager.getTransport(category);
        if (transport == null) {
            return ResponseEntity.badRequest().body("Unknown category: " + category);
        }
        return transport.handleMessage(sessionId, body);
    }
}
