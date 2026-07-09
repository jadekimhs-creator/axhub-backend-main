package io.shinhanlife.axhub.common.mcp.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @package io.shinhanlife.axhub.common.mcp.config
 * @className JacksonConfig
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
public class JacksonConfig {

    // 1. JSON 변환기(ObjectMapper)를 스프링 Bean으로 등록
    @Bean
    @Primary
    public ObjectMapper jsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    // 2. XML 변환기(XmlMapper)를 스프링 Bean으로 등록
    @Bean
    public XmlMapper xmlMapper() {
        return new XmlMapper();
    }
}