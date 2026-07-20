package io.shinhanlife.dap.mcg.config;


/**
 * @package io.shinhanlife.dap.mcg.config
 * @className AgentResponseBudgetProperties
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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mcp.agent-response")
public class AgentResponseBudgetProperties {
    private Integer maxPreviewItems;
    private Integer maxFieldsPerItem;
    private Long maxItemBytes;
    private Long maxTotalBytes;
    private Boolean includeSummary;

    public void setMaxPreviewItems(Integer maxPreviewItems) { this.maxPreviewItems = maxPreviewItems; }
    public void setMaxFieldsPerItem(Integer maxFieldsPerItem) { this.maxFieldsPerItem = maxFieldsPerItem; }
    public void setMaxItemBytes(Long maxItemBytes) { this.maxItemBytes = maxItemBytes; }
    public void setMaxTotalBytes(Long maxTotalBytes) { this.maxTotalBytes = maxTotalBytes; }
    public void setIncludeSummary(Boolean includeSummary) { this.includeSummary = includeSummary; }

    public int maxPreviewItems() { return maxPreviewItems == null || maxPreviewItems < 1 ? 20 : maxPreviewItems; }
    public int maxFieldsPerItem() { return maxFieldsPerItem == null || maxFieldsPerItem < 1 ? 10 : maxFieldsPerItem; }
    public long maxItemBytes() { return maxItemBytes == null || maxItemBytes < 1 ? 4_096 : maxItemBytes; }
    public long maxTotalBytes() { return maxTotalBytes == null || maxTotalBytes < 1 ? 65_536 : maxTotalBytes; }
    public boolean includeSummary() { return includeSummary == null || includeSummary; }
}
