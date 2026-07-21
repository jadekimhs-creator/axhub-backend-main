package io.shinhanlife.dap.biz.mcp.gateway.sync;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

class DynamicMcpControllerRouteTest {

    @Test
    void exposesOnlySupportedCategoryAwareMcpRoutes() {
        Set<String> getPaths = Arrays.stream(DynamicMcpController.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getAnnotationsByType(GetMapping.class)))
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .collect(Collectors.toSet());
        Set<String> postPaths = Arrays.stream(DynamicMcpController.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getAnnotationsByType(PostMapping.class)))
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .collect(Collectors.toSet());

        assertTrue(getPaths.contains("/mcp/sse/{category}"));
        assertTrue(postPaths.contains("/mcp/message/{category}"));
        assertTrue(postPaths.contains("/mcp/custom/{category}"));
        assertFalse(postPaths.contains("/mcp"));
        assertFalse(postPaths.contains("/mcp/initialize"));
        assertFalse(postPaths.contains("/mcp/sse/initialize"));
    }
}
