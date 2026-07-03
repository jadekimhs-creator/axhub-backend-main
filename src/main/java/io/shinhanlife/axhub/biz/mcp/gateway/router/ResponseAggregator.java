package io.shinhanlife.axhub.biz.mcp.gateway.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResponseAggregator {
    public Object normalize(Object rawResponse) {
        log.info("📊 [Aggregator] 결과 병합 및 응답 포맷 정규화");
        return rawResponse; // 최종 응답 반환
    }
}
