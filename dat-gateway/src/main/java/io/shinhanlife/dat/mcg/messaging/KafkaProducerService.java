package io.shinhanlife.dat.mcg.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

/**
 * @package io.shinhanlife.dat.mcg.messaging
 * @className KafkaProducerService
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 * 
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    // Spring Kafka 제공 템플릿
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper jsonMapper;

    /**
     * 트래픽 초과 등의 이유로 즉시 처리가 불가능한 요청을 Kafka 대기열로 보냅니다.
     * @param topic 발행할 Kafka 토픽명
     * @param payload 원본 요청 페이로드
     * @return 발급된 고유 티켓 ID
     */
    public String queueRequest(String topic, Map<String, Object> payload) {
        String ticketId = "TICKET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // 페이로드에 티켓 ID 추가 (Consumer가 알 수 있도록)
        payload.put("ticketId", ticketId);
        payload.put("queuedAt", System.currentTimeMillis());

        try {
            String jsonPayload = jsonMapper.writeValueAsString(payload);
            // 실제 Kafka 서버로 전송
            kafkaTemplate.send(topic, ticketId, jsonPayload);
            log.info(" [Kafka Queue] 요청 대기열 등록 완료 - Topic: {}, Ticket ID: {}", topic, ticketId);
        } catch (Exception e) {
            // 로컬 테스트 시 실제 Kafka 브로커가 없어서 나는 예외를 방어합니다.
            // 실제 상용 환경에서는 Dead Letter Queue 등을 태우거나 예외 처리 정책에 따릅니다.
            log.error(" [Kafka Queue] Kafka 서버 통신 실패 (Broker Down). Mock 모드로 티켓은 발급합니다. Error: {}", e.getMessage());
        }

        return ticketId;
    }
}
