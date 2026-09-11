package io.shinhanlife.dat.mcc.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.shinhanlife.dat.lib.mcp.McpRequestHeaderContext;
import io.shinhanlife.dat.lib.mcp.McpRequestHeaderFilter;
import io.shinhanlife.dat.lib.mcp.McpRequestHeaders;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class McpRequestHeaderFilterTest {

    @Test
    void capturesDapmsHeadersOnlyForTheCurrentRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.addHeader("x-request-id", "request-001");
        request.addHeader("guid", "guid-001");
        request.addHeader("mcp-session-id", "session-001");
        request.addHeader("employee-no", "ENC(employee)");
        request.addHeader("virtual-employee-no", "ENC(virtual)");

        new McpRequestHeaderFilter(null).doFilter(request, new MockHttpServletResponse(), (req, res) ->
                assertThat(asMap(McpRequestHeaderContext.current())).containsExactly(
                        Map.entry("requestId", "request-001"),
                        Map.entry("guid", "guid-001"),
                        Map.entry("mcpSessionId", "session-001"),
                        Map.entry("employeeNo", "ENC(employee)"),
                        Map.entry("virtualEmployeeNo", "ENC(virtual)")));

        assertNull(McpRequestHeaderContext.current());
    }

    private Map<String, Object> asMap(McpRequestHeaders headers) {
        try {
            Map<String, Object> values = new LinkedHashMap<>();
            for (RecordComponent component : McpRequestHeaders.class.getRecordComponents()) {
                values.put(component.getName(), component.getAccessor().invoke(headers));
            }
            return values;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
