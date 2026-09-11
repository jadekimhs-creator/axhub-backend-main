package io.shinhanlife.dat.lib.adapter.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Local HTTP mock server for scaffolded HTTP Tools.
 *
 * <p>Each Tool Pod returns the JSON generated under
 * {@code src/main/resources/mock-responses/{toolName}.json}. It is enabled only
 * when {@code axhub.mock.http.enabled=true}, which the HTTP Scaffold adds to local configuration.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "axhub.mock.http", name = "enabled", havingValue = "true")
@RequestMapping("/api")
public class MockEimsHttpServer {

    private final ObjectMapper objectMapper;

    @PostMapping("/mock/http/{toolName:[a-z0-9_-]+}")
    public ResponseEntity<JsonNode> mockToolHttpResponse(
            @PathVariable("toolName") String toolName,
            @RequestBody(required = false) JsonNode request) {
        ClassPathResource resource = new ClassPathResource("mock-responses/" + toolName + ".json");
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        try {
            log.info("[MockEimsHttpServer] HTTP mock request. toolName={}, body={}", toolName, request);
            return ResponseEntity.ok(objectMapper.readTree(resource.getInputStream()));
        } catch (Exception e) {
            log.warn("[MockEimsHttpServer] Unable to read mock response. toolName={}", toolName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/gateway")
    public ResponseEntity<?> mockEimsReceiver(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestBody Map<String, Object> request) {
        String interfaceId = String.valueOf(request.getOrDefault("interfaceId", ""));
        log.info("[MockEimsHttpServer] Legacy gateway mock request. traceId={}, interfaceId={}", traceId, interfaceId);
        return ResponseEntity.ok(Map.of(
                "status", "404",
                "message", "MOCK data is not defined for interfaceId: " + interfaceId));
    }
}
