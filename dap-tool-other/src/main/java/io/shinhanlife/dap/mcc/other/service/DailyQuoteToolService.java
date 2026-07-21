package io.shinhanlife.dap.mcc.other.service;

import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import io.shinhanlife.dap.mcc.service.AbstractMcpToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * @package io.shinhanlife.dap.mcc.service
 * @className DailyQuoteToolService
 * @description 랜덤 명언 제공 툴
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
@Service
@McpTool(
    routingType = "DIRECT",
    categoryKey = "sample"
)
public class DailyQuoteToolService extends AbstractMcpToolService {

    public record DailyQuoteReq(String category) {}
    public record DailyQuoteRes(String quote, String author) {}

    private final List<DailyQuoteRes> quotes = List.of(
        new DailyQuoteRes("성공은 매일 반복한 작은 노력들의 합이다.", "로버트 콜리어"),
        new DailyQuoteRes("시작이 반이다.", "아리스토텔레스"),
        new DailyQuoteRes("포기하지 않는 한 실패는 없다.", "알베르트 아인슈타인"),
        new DailyQuoteRes("가장 큰 위험은 위험 없는 삶이다.", "스티븐 코비")
    );

    @McpFunction(register = false, displayName = "랜덤 명언 툴", name = "daily_quote",
        description = "무작위로 영감을 주는 명언을 하나 가져옵니다.",
        prompt = "오늘의 명언 하나 알려줘, 동기부여 명언 등",
        mappingId = "QUOTE_001"
    )
    public DailyQuoteRes execute(DailyQuoteReq req) {
        int index = new Random().nextInt(quotes.size());
        DailyQuoteRes selected = quotes.get(index);
        
        log.info("[DailyQuoteTool] 명언 제공 완료: {}", selected.author());
        return selected;
    }
}
