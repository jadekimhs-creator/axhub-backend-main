import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class McpBridge {

    public static void main(String[] args) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Simple JSON parsing by finding method names
                // Using a rudimentary JSON parser since we don't have Jackson here
                if (line.contains("\"method\":\"initialize\"") || line.contains("\"method\": \"initialize\"")) {
                    String id = extractId(line);
                    String resp = "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{\"tools\":{}},\"serverInfo\":{\"name\":\"axhub-gateway\",\"version\":\"1.0.0\"}}}";
                    System.out.println(resp);
                    System.out.flush();
                } else if (line.contains("\"method\":\"notifications/initialized\"") || line.contains("\"method\": \"notifications/initialized\"")) {
                    // Do nothing
                } else if (line.contains("\"method\":\"tools/list\"") || line.contains("\"method\": \"tools/list\"")) {
                    String id = extractId(line);
                    try {
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create("http://localhost:8081/mcp/api/v1/tools/list"))
                                .header("X-API-KEY", "SHINHAN_MCP_TEST_KEY_9999")
                                .GET()
                                .build();
                        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
                        
                        // Parse tools from response string manually (basic string manipulation)
                        // axhub-gateway returns: {"result":{"tools":[{"subToolName":"other_get_sample_string","description":"...","parametersSchema":{...}}]}}
                        // We need to format it to standard MCP format
                        String rawJson = response.body();
                        // For simplicity, we can just proxy the response if it matches closely, but it doesn't.
                        // Let's just do a naive conversion by replacing "subToolName" with "name" and "parametersSchema" with "inputSchema"
                        String transformed = rawJson.replace("\"subToolName\"", "\"name\"").replace("\"parametersSchema\"", "\"inputSchema\"");
                        // Add type: "object" to inputSchema if missing - too complex with string replace, let's hope axhub-gateway already provides correct schema
                        
                        // The result format expects: {"tools": [ ... ]}
                        // axhub-gateway returns: {"id":"...","result":{"tools":[...]}}
                        // So we extract the "result" object and wrap it in the JSON-RPC response
                        int resultIdx = transformed.indexOf("\"result\":{");
                        if (resultIdx != -1) {
                            String resultObj = transformed.substring(resultIdx + 9, transformed.lastIndexOf("}") );
                            System.out.println("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":" + resultObj + "}");
                        } else {
                            System.out.println("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{\"tools\":[]}}");
                        }
                    } catch (Exception e) {
                        System.out.println("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"error\":{\"code\":-32603,\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}}");
                    }
                    System.out.flush();
                } else if (line.contains("\"method\":\"tools/call\"") || line.contains("\"method\": \"tools/call\"")) {
                    String id = extractId(line);
                    try {
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create("http://localhost:8081/mcp/api/v1/tools/call"))
                                .header("Content-Type", "application/json")
                                .header("X-Agent-Id", "Antigravity")
                                .header("X-API-KEY", "SHINHAN_MCP_TEST_KEY_9999")
                                .POST(HttpRequest.BodyPublishers.ofString(line))
                                .build();
                        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
                        
                        String rawJson = response.body();
                        // The gateway returns {"jsonrpc":"2.0", "result": {...}, "error": {...}}
                        // Standard MCP expects {"content": [{"type": "text", "text": "..."}], "isError": false}
                        
                        boolean isError = rawJson.contains("\"error\":") || rawJson.contains("\"status\":\"ERROR\"");
                        String escapedRawJson = rawJson.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
                        
                        System.out.println("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"" + escapedRawJson + "\"}],\"isError\":" + isError + "}}");
                    } catch (Exception e) {
                        System.out.println("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}],\"isError\":true}}");
                    }
                    System.out.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String extractId(String line) {
        int idx = line.indexOf("\"id\":");
        if (idx == -1) return "null";
        int start = idx + 5;
        while (start < line.length() && line.charAt(start) == ' ') {
            start++;
        }
        if (start >= line.length()) return "null";

        int end = start;
        if (line.charAt(start) == '\"') {
            end = start + 1;
            while (end < line.length() && line.charAt(end) != '\"') {
                end++;
            }
            if (end < line.length()) {
                end++; // include closing quote
            }
        } else {
            while (end < line.length() && Character.isDigit(line.charAt(end))) {
                end++;
            }
        }

        if (start == end) return "null";
        return line.substring(start, end);
    }
}
