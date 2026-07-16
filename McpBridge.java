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
                            // Spring AI MCP Serverì˜ ê³µì‹ ë‹¨ì¼ ì—”ë“œí¬ì¸íŠ¸
                            .uri(URI.create("http://localhost:8081/mcp"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(line))
                            .build();
                            
                    HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
                    System.out.println(response.body());
                    System.out.flush();
                } catch (Exception e) {
                    // MCP í‘œì¤€ ì—ëŸ¬ í¬ë§·ìœ¼ë¡œ ë°˜í™˜ (ì„ì˜ë¡œ id ì¶”ì¶œ ì œì™¸, ìµœì†Œí•œì˜ ì—ëŸ¬ ì‘ë‹µ)
                    System.out.println("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}}");
                    System.out.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}




# --- ½ÅÇÑ¶óÀÌÇÁ EAI/MCI ¿¬°è IP Á¤º¸ (°³¹ß È¯°æ) ---
shinhan.integration.envrTypeCd=D
shinhan.integration.eai.url=http://10.176.32.181
shinhan.integration.internalMci.url=http://10.176.32.173
shinhan.integration.bancaMci.url=http://10.176.32.117
shinhan.integration.externalMci.url=http://10.176.32.176

# --- ½ÅÇÑ¶óÀÌÇÁ EAI/MCI ¿¬°è IP Á¤º¸ (Å×½ºÆ® È¯°æ) ---
shinhan.integration.envrTypeCd=T
shinhan.integration.eai.url=http://10.174.32.181
shinhan.integration.internalMci.url=http://10.174.32.173
shinhan.integration.bancaMci.url=http://10.174.32.117
shinhan.integration.externalMci.url=http://10.176.32.177

# --- ½ÅÇÑ¶óÀÌÇÁ EAI/MCI ¿¬°è IP Á¤º¸ (¿î¿µ È¯°æ) ---
shinhan.integration.envrTypeCd=R
shinhan.integration.eai.url=http://10.172.32.181
shinhan.integration.internalMci.url=http://10.172.32.173
shinhan.integration.bancaMci.url=http://10.172.32.117
shinhan.integration.externalMci.url=http://10.172.32.177
