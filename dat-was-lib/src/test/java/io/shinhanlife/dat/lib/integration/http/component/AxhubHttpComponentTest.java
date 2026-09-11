package io.shinhanlife.dat.lib.integration.http.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dat.lib.config.GlowCommunicationProperties;
import io.shinhanlife.dat.lib.mcp.McpRequestHeaderContext;
import io.shinhanlife.dat.lib.mcp.McpRequestHeaders;
import io.shinhanlife.glow.communication.module.http.component.GlowHttpComponent;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AxhubHttpComponentTest {

    @Test
    void callByApiNameBuildsGlowTransferAndDeserializesJsonResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GlowHttpComponent glowHttpComponent = new GlowHttpComponent(builder);
        AxhubHttpProperties properties = new AxhubHttpProperties();
        properties.setApiList(List.of(new AxhubHttpProperties.ApiDefinition(
                "status", "https://api.example.test", "/v1", HttpMethod.GET, "application/json", false)));
        AxhubHttpComponent component = new AxhubHttpComponent(
                glowHttpComponent, new ObjectMapper(), new GlowCommunicationProperties(), properties);

        server.expect(requestTo("https://api.example.test/v1/status"))
                .andExpect(header("X-ANONYMOUS-REQ", "AXHUB-TOOL"))
                .andRespond(withSuccess("{\"status\":\"OK\"}", APPLICATION_JSON));

        SampleResponse response = component.call("status", "/status", null, SampleResponse.class);

        assertThat(response.status()).isEqualTo("OK");
        server.verify();
    }

    @Test
    void callByApiNameUsesConfiguredUrlMethodContentTypeAndBizPodHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GlowHttpComponent glowHttpComponent = new GlowHttpComponent(builder);
        AxhubHttpProperties properties = new AxhubHttpProperties();
        properties.setApiList(List.of(new AxhubHttpProperties.ApiDefinition(
                "employee", "https://employee.example.test", "/itrf/employee", HttpMethod.POST,
                "application/json;charset=UTF-8", true)));
        AxhubHttpComponent component = new AxhubHttpComponent(
                glowHttpComponent, new ObjectMapper(), new GlowCommunicationProperties(), properties);

        server.expect(requestTo("https://employee.example.test/itrf/employee"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(header("X-POD-TO-POD", "true"))
                .andRespond(withSuccess("{\"status\":\"OK\"}", APPLICATION_JSON));

        SampleResponse response = component.call("employee", "{\"employeeId\":\"EMP10001\"}", SampleResponse.class);

        assertThat(response.status()).isEqualTo("OK");
        server.verify();
    }

    @Test
    void forwardsDapmsHeadersToTheConfiguredHttpService() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AxhubHttpProperties properties = new AxhubHttpProperties();
        properties.setApiList(List.of(new AxhubHttpProperties.ApiDefinition(
                "status", "https://api.example.test", "/v1", HttpMethod.POST, "application/json", false)));
        AxhubHttpComponent component = new AxhubHttpComponent(
                new GlowHttpComponent(builder), new ObjectMapper(), new GlowCommunicationProperties(), properties);

        server.expect(requestTo("https://api.example.test/v1"))
                .andExpect(header("x-request-id", "request-1"))
                .andExpect(header("guid", "guid-1"))
                .andExpect(header("mcp-session-id", "session-1"))
                .andExpect(header("employee-no", "ENC(employee)"))
                .andExpect(header("virtual-employee-no", "ENC(virtual)"))
                .andRespond(withSuccess("{\"status\":\"OK\"}", APPLICATION_JSON));

        setRequestHeaders(headers(Map.of(
                "requestId", "request-1",
                "guid", "guid-1",
                "mcpSessionId", "session-1",
                "employeeNo", "ENC(employee)",
                "virtualEmployeeNo", "ENC(virtual)",
                "headerRequestId", "request-1",
                "traceId", "guid-1",
                "encryptedEmployeeId", "ENC(employee)")));
        try {
            assertThat(component.call("status", Map.of(), SampleResponse.class).status()).isEqualTo("OK");
            server.verify();
        } finally {
            clearRequestHeaders();
        }
    }

    private McpRequestHeaders headers(Map<String, String> values) {
        try {
            Class<?>[] types = Arrays.stream(McpRequestHeaders.class.getRecordComponents())
                    .map(component -> component.getType())
                    .toArray(Class<?>[]::new);
            Object[] arguments = Arrays.stream(McpRequestHeaders.class.getRecordComponents())
                    .map(component -> values.get(component.getName()))
                    .toArray();
            return McpRequestHeaders.class.getDeclaredConstructor(types).newInstance(arguments);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private void setRequestHeaders(McpRequestHeaders headers) throws Exception {
        Method method = McpRequestHeaderContext.class.getDeclaredMethod("set", McpRequestHeaders.class);
        method.setAccessible(true);
        method.invoke(null, headers);
    }

    private void clearRequestHeaders() throws Exception {
        Method method = McpRequestHeaderContext.class.getDeclaredMethod("clear");
        method.setAccessible(true);
        method.invoke(null);
    }

    record SampleResponse(String status) {
    }
}
