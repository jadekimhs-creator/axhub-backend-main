package io.shinhanlife.dap.biz.mcp.gateway.tool.large;

import io.shinhanlife.dap.biz.mcp.gateway.config.McpGatewayProperties;
import io.shinhanlife.dap.biz.mcp.gateway.guardrail.SensitiveDataMasker;
import io.shinhanlife.dap.biz.mcp.gateway.resilience.FailureType;
import io.shinhanlife.dap.biz.mcp.gateway.resilience.ToolExecutionException;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.Instant;

@Service
public class LargeToolResponseService {
    private final McpGatewayProperties properties;
    private final ObjectMapper json;
    private final SensitiveDataMasker masker;
    private final AgentResponseBudgetService agentBudget;

    public LargeToolResponseService(McpGatewayProperties properties,
                                    ObjectMapper json,
                                    SensitiveDataMasker masker,
                                    AgentResponseBudgetService agentBudget) {
        this.properties = properties;
        this.json = json;
        this.masker = masker;
        this.agentBudget = agentBudget;
    }

    public Collector newCollector(String toolName, String requestId) {
        return new Collector(toolName, requestId);
    }

    public ObjectNode tooLargeResult(String toolName, String requestId, long receivedBytes, String nextCursor) {
        ObjectNode response = basePartialResponse(toolName, requestId);
        response.put("failureType", FailureType.TOO_LARGE_RESULT.name());
        response.put("errorCode", "TOOL_RESULT_TOO_LARGE");
        response.put("message", "Tool 응답이 허용 크기를 초과했습니다. 원문 body는 저장하지 않았고 cursor/resultRef 기반 재조회가 필요합니다.");
        response.put("receivedBytes", receivedBytes);
        response.put("maxResponseBytesFromTool", properties.maxResponseBytesFromTool());
        response.put("hasMore", true);
        response.put("nextCursor", safe(nextCursor));
        response.put("resultRef", resultRef(toolName, requestId, nextCursor));
        response.put("truncated", true);
        response.put("omittedCount", 0);
        response.set("previewItems", json.createArrayNode());
        response.set("streamEvents", streamEvents(toolName, 0, 0, true, nextCursor));
        attachPageInfoAndStructuredContent(response);
        return agentBudget.apply(response);
    }

    private ObjectNode basePartialResponse(String toolName, String requestId) {
        ObjectNode response = json.createObjectNode();
        response.put("success", true);
        response.put("toolName", toolName);
        response.put("requestId", requestId);
        response.put("status", "PARTIAL_RESULT");
        response.put("pageSize", pageSize());
        response.put("pageCount", 0);
        response.put("returnedCount", 0);
        response.put("totalCount", 0);
        response.put("hasMore", false);
        response.put("nextCursor", "");
        response.put("truncated", false);
        response.put("omittedCount", 0);
        response.put("maxStreamBytes", properties.maxStreamBytes());
        response.put("maxItemBytes", properties.largeResponseMaxItemBytes());
        return response;
    }

    private ArrayNode streamEvents(String toolName, int pageCount, int returnedCount, boolean hasMore, String nextCursor) {
        ArrayNode events = json.createArrayNode();
        ObjectNode start = json.createObjectNode();
        start.put("type", "start");
        start.put("tool", toolName);
        events.add(start);

        ObjectNode summary = json.createObjectNode();
        summary.put("type", "summary");
        summary.put("pageCount", pageCount);
        summary.put("returnedCount", returnedCount);
        summary.put("message", returnedCount + "건의 preview를 생성했습니다.");
        events.add(summary);

        ObjectNode done = json.createObjectNode();
        done.put("type", "done");
        done.put("hasMore", hasMore);
        done.put("nextCursor", safe(nextCursor));
        events.add(done);
        return events;
    }

    private String resultRef(String toolName, String requestId, String nextCursor) {
        if (nextCursor == null || nextCursor.isBlank()) {
            return "tool-result:" + toolName + ":" + requestId;
        }
        return "tool-result:" + toolName + ":" + requestId + ":cursor:" + nextCursor;
    }

    private int pageSize() {
        return Math.min(properties.defaultPageSize(), properties.maxPageSize());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public class Collector {
        private final String toolName;
        private final String requestId;
        private final Instant startedAt = Instant.now();
        private final ArrayNode previewItems = json.createArrayNode();
        private final ArrayNode streamEvents = json.createArrayNode();
        private int pageCount;
        private int returnedCount;
        private long totalCount;
        private boolean paginated;
        private boolean hasMore;
        private String nextCursor = "";
        private String stopReason = "";
        private JsonNode normalData;
        private boolean truncated;
        private int omittedCount;

        private Collector(String toolName, String requestId) {
            this.toolName = toolName;
            this.requestId = requestId;
            ObjectNode start = json.createObjectNode();
            start.put("type", "start");
            start.put("tool", toolName);
            streamEvents.add(start);
        }

        public void accept(JsonNode data) {
            if (data == null || data.isMissingNode() || data.isNull()) {
                throw new ToolExecutionException(FailureType.BUSINESS_ERROR, "Tool response data is empty. tool=" + toolName);
            }
            Page page = pageFrom(data);
            if (!page.paginated() && pageCount == 0 && count(page.items()) <= pageSize()) {
                JsonNode masked = masker.mask(data);
                normalData = masked;
                pageCount = 1;
                returnedCount = count(page.items());
                totalCount = returnedCount;
                return;
            }

            paginated = true;
            pageCount++;
            totalCount = page.totalCount() >= 0 ? page.totalCount() : totalCount;
            hasMore = page.hasMore();
            nextCursor = page.nextCursor();

            int pageItemCount = 0;
            for (JsonNode item : page.items()) {
                pageItemCount++;
                JsonNode preview = previewItem(item);
                if (previewItems.size() < maxPreviewItems() && canAddPreviewItem(preview)) {
                    previewItems.add(preview);
                    returnedCount++;
                } else {
                    omittedCount++;
                    truncated = true;
                }
            }

            ObjectNode summary = json.createObjectNode();
            summary.put("type", "summary");
            summary.put("page", pageCount);
            summary.put("count", pageItemCount);
            summary.put("returnedCount", returnedCount);
            summary.put("omittedCount", omittedCount);
            summary.put("nextCursor", nextCursor());
            summary.put("message", returnedCount + " items previewed");
            addStreamEvent(summary);

            if (pageCount >= properties.maxPagesPerCall() && hasMore) {
                stopReason = "MAX_PAGES_PER_CALL";
                truncated = true;
            }
            if (Duration.between(startedAt, Instant.now()).toSeconds() >= properties.maxDurationSeconds() && hasMore) {
                stopReason = "MAX_DURATION_SECONDS";
                truncated = true;
            }
        }

        public boolean shouldFetchNextPage() {
            return paginated
                    && hasMore
                    && nextCursor != null
                    && !nextCursor.isBlank()
                    && pageCount < properties.maxPagesPerCall()
                    && Duration.between(startedAt, Instant.now()).toSeconds() < properties.maxDurationSeconds()
                    && currentResponseBytes() < properties.maxStreamBytes();
        }

        public String nextCursor() {
            return safe(nextCursor);
        }

        public int pageSize() {
            return LargeToolResponseService.this.pageSize();
        }

        public ObjectNode finish() {
            if (normalData != null) {
                ObjectNode wrap = json.createObjectNode();
                wrap.put("success", true);
                wrap.put("toolName", toolName);
                wrap.put("requestId", requestId);
                wrap.set("data", normalData);
                return wrap;
            }

            ObjectNode done = json.createObjectNode();
            done.put("type", "done");
            done.put("totalCount", totalCount);
            done.put("hasMore", hasMore);
            done.put("nextCursor", nextCursor());
            addStreamEvent(done);

            ObjectNode response = basePartialResponse(toolName, requestId);
            response.put("pageCount", pageCount);
            response.put("returnedCount", returnedCount);
            response.put("totalCount", totalCount);
            response.put("hasMore", hasMore);
            response.put("nextCursor", nextCursor());
            response.put("resultRef", resultRef(toolName, requestId, nextCursor()));
            response.put("truncated", truncated);
            response.put("omittedCount", omittedCount);
            response.put("message", stopReason.isBlank()
                    ? "대량 결과라 일부 preview만 반환했습니다."
                    : "대량 결과라 " + stopReason + " 조건에서 중단하고 일부 preview만 반환했습니다.");
            response.set("previewItems", previewItems);
            response.set("streamEvents", streamEvents);
            attachPageInfoAndStructuredContent(response);
            return agentBudget.apply(response);
        }

        private Page pageFrom(JsonNode data) {
            if (data.isObject() && data.path("items").isArray()) {
                return new Page(true, data.path("items"), data.path("nextCursor").asText(""),
                        data.path("hasMore").asBoolean(false), data.path("totalCount").asLong(-1));
            }
            if (data.isArray()) {
                return new Page(false, data, "", false, count(data));
            }
            ArrayNode one = json.createArrayNode();
            one.add(data);
            return new Page(false, one, "", false, 1);
        }

        private JsonNode previewItem(JsonNode item) {
            JsonNode masked = masker.mask(item);
            long itemBytes = jsonBytes(masked);
            if (itemBytes <= properties.largeResponseMaxItemBytes()) {
                return masked;
            }
            truncated = true;
            ObjectNode preview = json.createObjectNode();
            preview.put("truncated", true);
            preview.put("originalBytes", itemBytes);
            preview.put("maxItemBytes", properties.largeResponseMaxItemBytes());
            if (masked.isObject()) {
                ArrayNode fieldNames = json.createArrayNode();
                java.util.Iterator<String> fieldNamesIter = masked.fieldNames();
                while (fieldNamesIter.hasNext()) {
                    fieldNames.add(fieldNamesIter.next());
                }
                preview.set("fieldNames", fieldNames);
            }
            return preview;
        }

        private boolean canAddPreviewItem(JsonNode item) {
            return jsonBytes(item) <= properties.largeResponseMaxItemBytes()
                    && currentResponseBytes() + jsonBytes(item) <= properties.maxStreamBytes();
        }

        private void addStreamEvent(ObjectNode event) {
            if (currentResponseBytes() + jsonBytes(event) <= properties.maxStreamBytes()) {
                streamEvents.add(event);
                return;
            }
            truncated = true;
        }

        private int maxPreviewItems() {
            return Math.min(pageSize() * properties.maxPagesPerCall(), properties.largeResponseMaxPreviewItems());
        }

        private long currentResponseBytes() {
            ObjectNode estimate = json.createObjectNode();
            estimate.set("previewItems", previewItems);
            estimate.set("streamEvents", streamEvents);
            return jsonBytes(estimate);
        }

        private long jsonBytes(JsonNode node) {
            try {
                return json.writeValueAsBytes(node).length;
            } catch (Exception error) {
                return Long.MAX_VALUE;
            }
        }

        private int count(JsonNode node) {
            int count = 0;
            java.util.Iterator<JsonNode> iter = node.elements();
            while(iter.hasNext()) {
                iter.next();
                count++;
            }
            return count;
        }
    }

    private record Page(boolean paginated, JsonNode items, String nextCursor, boolean hasMore, long totalCount) {}

    private void attachPageInfoAndStructuredContent(ObjectNode response) {
        ObjectNode pageInfo = json.createObjectNode();
        pageInfo.put("pageSize", response.path("pageSize").asInt(pageSize()));
        pageInfo.put("pageCount", response.path("pageCount").asInt(0));
        pageInfo.put("returnedCount", response.path("returnedCount").asInt(0));
        pageInfo.put("totalCount", response.path("totalCount").asLong(0));
        pageInfo.put("hasMore", response.path("hasMore").asBoolean(false));
        pageInfo.put("nextCursor", response.path("nextCursor").asText(""));
        response.set("pageInfo", pageInfo);

        ObjectNode summary = json.createObjectNode();
        summary.put("status", response.path("status").asText("PARTIAL_RESULT"));
        summary.put("toolName", response.path("toolName").asText(""));
        summary.put("message", response.path("message").asText(""));
        summary.put("truncated", response.path("truncated").asBoolean(false));
        summary.put("omittedCount", response.path("omittedCount").asInt(0));
        summary.put("resultRef", response.path("resultRef").asText(""));

        ObjectNode structuredContent = json.createObjectNode();
        structuredContent.set("summary", summary);
        structuredContent.set("pageInfo", pageInfo.deepCopy());
        structuredContent.set("previewItems", response.path("previewItems").deepCopy());
        response.set("structuredContent", structuredContent);
    }
}
