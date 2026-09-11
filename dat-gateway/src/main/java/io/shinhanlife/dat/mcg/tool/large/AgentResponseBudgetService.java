package io.shinhanlife.dat.mcg.tool.large;


/**
 * @package io.shinhanlife.dat.mcg.tool.large
 * @className AgentResponseBudgetService
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
import java.util.Iterator;
import java.util.Map;

import io.shinhanlife.dat.mcg.config.AgentResponseBudgetProperties;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class AgentResponseBudgetService {
    private final AgentResponseBudgetProperties properties;
    private final ObjectMapper json;
    public AgentResponseBudgetService(AgentResponseBudgetProperties properties, ObjectMapper json) {
        this.properties = properties;
        this.json = json;
    }

    public ObjectNode apply(ObjectNode response) {
        ArrayNode originalItems = response.path("previewItems").isArray()
                ? (ArrayNode) response.path("previewItems")
                : json.createArrayNode();
        int originalCount = originalItems.size();
        BudgetStats stats = new BudgetStats();
        ArrayNode budgetItems = json.createArrayNode();

        for (JsonNode item : originalItems) {
            if (budgetItems.size() >= properties.maxPreviewItems()) {
                stats.omittedItems++;
                stats.limited = true;
                continue;
            }
            JsonNode budgetItem = budgetItem(item, stats);
            if (jsonBytes(budgetItem) > properties.maxItemBytes()) {
                budgetItem = fallbackTruncatedItem(budgetItem);
                stats.truncatedItems++;
                stats.limited = true;
            }
            budgetItems.add(budgetItem);
        }

        ObjectNode budgeted = copyWithout(response, "previewItems", "budget", "summary");
        budgeted.set("previewItems", budgetItems);
        if (properties.includeSummary()) {
            budgeted.put("summary", summary(budgeted, originalCount, budgetItems.size()));
        }

        while (jsonBytes(budgeted) > properties.maxTotalBytes() && budgetItems.size() > 0) {
            budgetItems.remove(budgetItems.size() - 1);
            stats.omittedItems++;
            stats.limited = true;
            budgeted.set("previewItems", budgetItems);
            if (properties.includeSummary()) {
                budgeted.put("summary", summary(budgeted, originalCount, budgetItems.size()));
            }
        }

        ObjectNode budget = json.createObjectNode();
        updateBudget(budget, stats, originalCount, budgetItems.size());
        budgeted.put("previewCount", budgetItems.size());
        budgeted.set("budget", budget);
        while (jsonBytes(budgeted) > properties.maxTotalBytes() && budgetItems.size() > 0) {
            budgetItems.remove(budgetItems.size() - 1);
            stats.omittedItems++;
            stats.limited = true;
            budgeted.set("previewItems", budgetItems);
            budgeted.put("previewCount", budgetItems.size());
            if (properties.includeSummary()) {
                budgeted.put("summary", summary(budgeted, originalCount, budgetItems.size()));
            }
            updateBudget(budget, stats, originalCount, budgetItems.size());
            budgeted.set("budget", budget);
        }
        return budgeted;
    }

    private void updateBudget(ObjectNode budget, BudgetStats stats, int originalCount, int previewCount) {
        budget.put("limited", stats.limited || stats.omittedItems > 0 || stats.truncatedItems > 0);
        budget.put("maxPreviewItems", properties.maxPreviewItems());
        budget.put("maxFieldsPerItem", properties.maxFieldsPerItem());
        budget.put("maxItemBytes", properties.maxItemBytes());
        budget.put("maxTotalBytes", properties.maxTotalBytes());
        budget.put("originalPreviewCount", originalCount);
        budget.put("previewCount", previewCount);
        budget.put("omittedItems", stats.omittedItems);
        budget.put("truncatedItems", stats.truncatedItems);
    }

    private JsonNode budgetItem(JsonNode item, BudgetStats stats) {
        JsonNode masked = item;
        if (!masked.isObject()) {
            return truncateByBytes(masked, stats);
        }
        ObjectNode limited = json.createObjectNode();
        int copied = 0;
        int originalFields = 0;
        
        Iterator<Map.Entry<String, JsonNode>> fields = masked.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            originalFields++;
            if (copied >= properties.maxFieldsPerItem()) {
                continue;
            }
            limited.set(entry.getKey(), entry.getValue());
            copied++;
        }
        
        if (originalFields > copied) {
            limited.put("truncated", true);
            limited.put("omittedFieldCount", originalFields - copied);
            stats.truncatedItems++;
            stats.limited = true;
        }
        return truncateByBytes(limited, stats);
    }

    private JsonNode truncateByBytes(JsonNode item, BudgetStats stats) {
        if (jsonBytes(item) <= properties.maxItemBytes()) {
            return item;
        }
        if (!item.isObject()) {
            stats.truncatedItems++;
            stats.limited = true;
            ObjectNode truncated = json.createObjectNode();
            truncated.put("truncated", true);
            truncated.put("originalBytes", jsonBytes(item));
            truncated.put("maxItemBytes", properties.maxItemBytes());
            return truncated;
        }
        ObjectNode source = (ObjectNode) item;
        ObjectNode limited = json.createObjectNode();
        
        Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                limited.put(entry.getKey(), truncateText(value.asText()));
            } else {
                limited.set(entry.getKey(), value);
            }
        }
        
        limited.put("truncated", true);
        limited.put("originalBytes", jsonBytes(item));
        limited.put("maxItemBytes", properties.maxItemBytes());
        stats.truncatedItems++;
        stats.limited = true;
        if (jsonBytes(limited) <= properties.maxItemBytes()) {
            return limited;
        }
        return fallbackTruncatedItem(item);
    }

    private ObjectNode fallbackTruncatedItem(JsonNode item) {
        ObjectNode fallback = json.createObjectNode();
        fallback.put("truncated", true);
        fallback.put("originalBytes", jsonBytes(item));
        fallback.put("maxItemBytes", properties.maxItemBytes());
        if (item.isObject()) {
            ArrayNode fieldNames = json.createArrayNode();
            Iterator<String> fieldNamesIter = item.fieldNames();
            while (fieldNamesIter.hasNext()) {
                fieldNames.add(fieldNamesIter.next());
            }
            fallback.set("fieldNames", fieldNames);
        }
        return fallback;
    }

    private String truncateText(String value) {
        if (value == null) {
            return "";
        }
        int maxChars = Math.min(value.length(), 256);
        String truncated = value.substring(0, maxChars);
        while (truncated.length() > 16 && truncated.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > properties.maxItemBytes() / 2) {
            truncated = truncated.substring(0, truncated.length() / 2);
        }
        return truncated + "...";
    }

    private ObjectNode copyWithout(ObjectNode source, String... excludedFields) {
        ObjectNode copy = json.createObjectNode();
        
        Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (!excluded(entry.getKey(), excludedFields)) {
                copy.set(entry.getKey(), entry.getValue());
            }
        }
        
        return copy;
    }

    private boolean excluded(String field, String[] excludedFields) {
        for (String excluded : excludedFields) {
            if (excluded.equals(field)) {
                return true;
            }
        }
        return false;
    }

    private String summary(ObjectNode response, int originalCount, int previewCount) {
        long totalCount = response.path("totalCount").asLong(originalCount);
        return "Returned " + previewCount + " preview item(s) out of " + totalCount + " total result(s).";
    }

    private long jsonBytes(JsonNode node) {
        try {
            return json.writeValueAsBytes(node).length;
        } catch (Exception error) {
            return Long.MAX_VALUE;
        }
    }

    private static final class BudgetStats {
        private boolean limited;
        private int omittedItems;
        private int truncatedItems;
    }
}
