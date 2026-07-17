/**
 * @package io.shinhanlife
 * @className McpBridge
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
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
                            // Spring AI MCP Server??怨듭떇 ?⑥씪 ?붾뱶?ъ씤??
                            .uri(URI.create("http://localhost:8081/mcp"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(line))
                            .build();
                            
                    HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
                    System.out.println(response.body());
                    System.out.flush();
                } catch (Exception e) {
                    // MCP ?쒖? ?먮윭 ?щ㎎?쇰줈 諛섑솚 (?꾩쓽濡?id 異붿텧 ?쒖쇅, 理쒖냼?쒖쓽 ?먮윭 ?묐떟)
                    System.out.println("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}}");
                    System.out.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
