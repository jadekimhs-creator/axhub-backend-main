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

                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            // Spring AI MCP Server의 공식 단일 엔드포인트
                            .uri(URI.create("http://localhost:8081/mcp"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(line))
                            .build();
                            
                    HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
                    System.out.println(response.body());
                    System.out.flush();
                } catch (Exception e) {
                    // MCP 표준 에러 포맷으로 반환 (임의로 id 추출 제외, 최소한의 에러 응답)
                    System.out.println("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}}");
                    System.out.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



