package io.shinhanlife.axhub.biz.mcp.gateway.config;

import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @package io.shinhanlife.axhub.biz.mcp.gateway.config
 * @className RedisConfig
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
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ToolMetadata> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ToolMetadata> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 1. Key는 무조건 String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 2. Value는 Generic 직렬화기 사용 (생성자 인자 없음)
        Jackson2JsonRedisSerializer<ToolMetadata> serializer = new Jackson2JsonRedisSerializer<>(ToolMetadata.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}