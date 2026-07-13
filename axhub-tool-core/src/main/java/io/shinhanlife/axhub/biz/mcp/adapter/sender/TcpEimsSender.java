package io.shinhanlife.axhub.biz.mcp.adapter.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @package io.shinhanlife.axhub.biz.mcp.adapter.sender
 * @className TcpEimsSender
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
@Slf4j
@Component
public class TcpEimsSender implements EimsSender {

    @Value("${eims.tcp.host}")
    private String host;

    @Value("${eims.tcp.port}")
    private int port;

    @Value("${eims.tcp.timeout}")
    private int timeout;

    @Override
    public String send(String interfaceId, String payload) throws Exception {
        log.info(" [TCP 소켓 모드] EIMS 접속 중... {}:{}", host, port);

        // TCP 소켓 자원을 사용 후 안전하게 닫아주는 try-with-resources 구문
        try (Socket socket = new Socket()) {
            // 1. 타임아웃 및 연결 설정
            socket.connect(new InetSocketAddress(host, port), timeout);
            socket.setSoTimeout(timeout); // 읽기 타임아웃

            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();

            // 2. 데이터 송신 (EUC-KR 인코딩 필수)
            // 보통 금융권 TCP 통신은 맨 앞에 전체 길이나 인터페이스 ID를 헤더로 붙입니다.
            String sendData = interfaceId + payload; 
            os.write(sendData.getBytes("EUC-KR"));
            os.flush();

            // 3. 데이터 수신
            byte[] buffer = new byte[4096];
            int readByte = is.read(buffer);

            if (readByte == -1) {
                throw new RuntimeException("EIMS 서버가 응답 없이 연결을 종료했습니다.");
            }

            // 받은 바이트를 다시 한글(EUC-KR) 문자열로 복원
            return new String(buffer, 0, readByte, "EUC-KR");
        }
    }
}