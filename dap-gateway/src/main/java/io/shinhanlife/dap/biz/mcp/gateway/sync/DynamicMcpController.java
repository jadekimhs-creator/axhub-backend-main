package io.shinhanlife.dap.biz.mcp.gateway.sync;

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

    @PostMapping("/mcp")
    public SseEmitter handleSse1() {
        System.out.println("11111111111");
        return null;
    }

    @PostMapping("/mcp/initialize")
    public SseEmitter handleSse2() {
        System.out.println("2222222222");
        return null;
    }

    @PostMapping("/mcp/sse/initialize")
    public SseEmitter handleSse3() {
        System.out.println("33333333333333");
        return null;
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
