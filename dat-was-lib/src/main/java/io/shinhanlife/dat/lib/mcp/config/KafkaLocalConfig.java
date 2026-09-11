package io.shinhanlife.dat.lib.mcp.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
import java.util.Map;

/**
 * @package io.shinhanlife.dat.lib.mcp.config
 * @className KafkaLocalConfig
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
@Configuration
public class KafkaLocalConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // 1. 카프카 전송 공장(Factory) 세팅
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        // 가짜 로컬 주소 혹은 환경변수 세팅
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // 데이터를 카프카로 보낼 때 문자열(String) 형태로 변환하겠다는 규칙
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // 3초 만에 빠른 실패 처리 (로컬 무한 대기 방지)
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000);
        // 재접속 주기를 10초로 설정 (콘솔 로그 도배 방지)
        configProps.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10000);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    // 2. EaiEimsSender가 애타게 찾던 KafkaTemplate을 스프링 Bean으로 등록!
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}