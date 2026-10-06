package io.shinhanlife.dat.lib.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.shinhanlife.dat.lib.annotation.GrowToolHint;
import io.shinhanlife.dat.lib.config.McpProperties;
import io.shinhanlife.dat.lib.util.ToolSchemaResolver;
import jakarta.annotation.PreDestroy;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

class ToolRegistryHeartbeatSenderTest {

    @Test
    void applicationReadyDoesNotPushToolsToGateway() throws Exception {
        try (GatewayProbe gateway = new GatewayProbe()) {
            ToolRegistryHeartbeatSender sender = senderWithOneTool(gateway.url());

            sender.init();
            invokeLifecycleMethods(sender, EventListener.class);

            assertThat(gateway.receivedRequestWithin(Duration.ofMillis(300))).isFalse();
        }
    }

    @Test
    void shutdownDoesNotDeregisterToolsFromGateway() throws Exception {
        try (GatewayProbe gateway = new GatewayProbe()) {
            ToolRegistryHeartbeatSender sender = senderWithOneTool(gateway.url());

            sender.init();
            invokeLifecycleMethods(sender, PreDestroy.class);

            assertThat(gateway.receivedRequestWithin(Duration.ofMillis(300))).isFalse();
        }
    }

    @Test
    void readsResilienceValuesOnlyFromGrowToolHint() throws Exception {
        ToolRegistryHeartbeatSender sender = senderWithOneTool("http://127.0.0.1:1");

        sender.init();

        var metadata = sender.getAllScannedTools().getFirst();
        assertThat(metadata.getTimeoutMillis()).isEqualTo(12000L);
        assertThat(metadata.getRetryMaxAttempts()).isEqualTo(1);
    }

    private ToolRegistryHeartbeatSender senderWithOneTool(String gatewayUrl) throws Exception {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(Object.class)).thenReturn(Map.of("tool", new SampleTool()));
        ToolRegistryHeartbeatSender sender = new ToolRegistryHeartbeatSender(
                applicationContext, new ObjectMapper(), new McpProperties(), mock(ToolSchemaResolver.class));
        setFieldIfPresent(sender, "gatewayUrl", gatewayUrl);
        setField(sender, "podUrl", "http://localhost:8084");
        return sender;
    }

    private void invokeLifecycleMethods(Object target, Class<? extends Annotation> annotationType) throws Exception {
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (method.getAnnotation(annotationType) == null) {
                continue;
            }
            if (method.getParameterCount() == 0) {
                method.invoke(target);
            } else if (method.getParameterTypes()[0] == ApplicationReadyEvent.class) {
                method.invoke(target, mock(ApplicationReadyEvent.class));
            }
        }
    }

    private void setField(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void setFieldIfPresent(Object target, String name, Object value) throws Exception {
        try {
            setField(target, name, value);
        } catch (NoSuchFieldException ignored) {
            // The desired implementation has no Gateway registration configuration.
        }
    }

    static class SampleTool {
        @McpTool(name = "test_sample_tool")
        @GrowToolHint(timeoutMillis = 12000L, retryMaxAttempts = 1)
        void execute() {
        }
    }

    private static final class GatewayProbe implements AutoCloseable {
        private final HttpServer server;
        private final CountDownLatch requestReceived = new CountDownLatch(1);

        private GatewayProbe() throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                requestReceived.countDown();
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            });
            server.start();
        }

        private String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private boolean receivedRequestWithin(Duration timeout) throws InterruptedException {
            return requestReceived.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
