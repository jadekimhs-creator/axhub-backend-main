package io.shinhanlife.axhub.common.mcp.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    //  스프링이 캐시를 관리할 기본 저장소를 빈(Bean)으로 등록합니다.
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("eimsData"); // 아까 설정한 캐시 이름 등록
    }
}