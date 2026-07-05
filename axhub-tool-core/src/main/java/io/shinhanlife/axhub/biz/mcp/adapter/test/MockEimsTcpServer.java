package io.shinhanlife.axhub.biz.mcp.adapter.test;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import java.util.Map;

@Slf4j
@Component
// 대신 "local" 환경(application-local.properties)에서만 가짜 소켓 서버가 켜지도록 보장합니다.
@Profile("local")
public class MockEimsTcpServer {

    private final ObjectMapper objectMapper;

    public MockEimsTcpServer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Value("${eims.tcp.port:8090}")
    private int port;

    @Value("${server.port:8081}")
    private int serverPort;

    private ServerSocket serverSocket;
    private boolean running = true;

    @EventListener(ApplicationReadyEvent.class)
    public void startTcpServer() {
        // MSA 환경에서는 각 툴 Pod 내부에서 독립적인 가짜 EIMS 서버가 실행되도록 허용
        log.info(" [가짜 EIMS 서버] 로컬 테스트 환경용 EIMS TCP 서버 가동을 준비합니다.");

        // Java 21 가상 스레드를 사용하여 메인 서버 가동에 방해 없이 백그라운드에서 실행
        Thread.ofVirtual().start(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log.info(" [가짜 EIMS 서버] 로컬 TCP 소켓 서버 가동 완료 (Port: {})", port);

                while (running) {
                    Socket clientSocket = serverSocket.accept();

                    // 연결된 요청을 별도 스레드로 처리
                    Thread.ofVirtual().start(() -> handleClient(clientSocket));
                }
            } catch (Exception e) {
                if (running) log.error(" 가짜 TCP 서버 에러: {}", e.getMessage());
            }
        });
    }

    private void handleClient(Socket socket) {
        try (socket) {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            byte[] buffer = new byte[4096];
            int readByte = is.read(buffer);

            if (readByte != -1) {
                String receivedData = new String(buffer, 0, readByte, "EUC-KR");
                log.info(" [가짜 EIMS 서버] TCP 데이터 수신 완료!");
                log.info(" 수신된 전문: [{}]", receivedData);

                String responseData = "{\"status\":\"404\",\"message\":\"MOCK 데이터가 정의되지 않았습니다.\"}";
                try {
                    int braceIndex = receivedData.indexOf('{');
                    String interfaceId = "";
                    if (braceIndex > 0) {
                        interfaceId = receivedData.substring(0, braceIndex).trim();
                    } else if (braceIndex == -1) {
                        interfaceId = receivedData.trim();
                    }

                    ClassPathResource resource = new ClassPathResource("mock-responses.json");
                    Map<String, Object> mockDataMap = objectMapper.readValue(resource.getInputStream(), Map.class);

                    if (mockDataMap.containsKey(interfaceId)) {
                        responseData = objectMapper.writeValueAsString(mockDataMap.get(interfaceId));
                    }
                } catch (Exception e) {
                    log.warn("TCP JSON 파싱 에러 또는 파일 읽기 실패", e);
                }

                // 응답 데이터 송신
                os.write(responseData.getBytes("EUC-KR"));
                os.flush();
            }
        } catch (Exception e) {
            log.error(" 가짜 TCP 클라이언트 처리 에러: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stopTcpServer() {
        this.running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
    }
}