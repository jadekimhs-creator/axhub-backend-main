package io.shinhanlife.dat.mcg.presentation;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * @package io.shinhanlife.dat.mcg.presentation
 * @className ToolReportProxyController
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
@RestController
public class ToolReportProxyController {

    private final RestClient restClient;
    private final String reportServiceUrl;

    public ToolReportProxyController(RestClient.Builder restClientBuilder,
                                     @Value("${report.service-url}") String reportServiceUrl) {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(300_000);
        this.restClient = restClientBuilder.requestFactory(factory).build();
        this.reportServiceUrl = reportServiceUrl.replaceAll("/+$", "");
    }

    @GetMapping(value = "/report/api/report-tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ReportToolSummary> tools(
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "projectPath", required = false) String projectPath) {
        try {
            org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(reportServiceUrl + "/api/report-tools");
            if (projectPath != null && !projectPath.isBlank()) {
                builder.queryParam("projectPath", projectPath.replace('\\', '/'));
            } else {
                builder.queryParam("sourceType", sourceType == null ? "DAP_WAS_DAPMT" : sourceType);
            }
            ReportToolSummary[] tools = restClient.get()
                    .uri(builder.build().encode().toUri())
                    .retrieve()
                    .body(ReportToolSummary[].class);
            return tools == null ? List.of() : List.of(tools);
        } catch (Exception e) {
            throw new IllegalStateException("Tool Report 서비스(" + reportServiceUrl + ")에 연결할 수 없습니다. "
                    + "dap-tool-report 서비스가 실행 중인지 확인해 주세요. (" + e.getMessage() + ")", e);
        }
    }

    @PostMapping(value = "/report/api/tool-reports/excel",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/zip",
                    "application/octet-stream"
            })
    public ResponseEntity<byte[]> excel(@RequestBody Map<String, Object> request) {
        Map<String, Object> body = new java.util.LinkedHashMap<>(request);
        if (body.get("projectPath") instanceof String p && !p.isBlank()) {
            body.put("projectPath", p.replace('\\', '/'));
        }
        ResponseEntity<byte[]> response = restClient.post()
                .uri(reportServiceUrl + "/api/tool-reports/excel")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(byte[].class);
        return copy(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleError(Exception e) {
        if (e instanceof RestClientResponseException rre) {
            String respBody = rre.getResponseBodyAsString();
            return ResponseEntity.status(rre.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", respBody != null && !respBody.isBlank() ? respBody : "Downstream error: " + rre.getMessage()));
        }
        if (e instanceof RestClientException) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Tool Report 서비스(" + reportServiceUrl + ")에 연결할 수 없습니다. dap-tool-report 서비스가 실행 중인지 확인해 주세요. (" + e.getMessage() + ")"
            ));
        }
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage() == null ? "Tool Report 서비스 오류" : e.getMessage()));
    }

    private ResponseEntity<byte[]> copy(ResponseEntity<byte[]> response) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.getStatusCode());
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType != null) builder.contentType(contentType);
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (disposition != null) builder.header(HttpHeaders.CONTENT_DISPOSITION, disposition);
        return builder.body(response.getBody());
    }

    public record ReportToolSummary(
            String name, String title, String description, String categoryKey, String mappingId,
            boolean register, boolean requiresApproval, boolean readOnlyHint, boolean destructiveHint,
            boolean idempotentHint, boolean openWorldHint, String requestType, String responseType,
            String useCaseClass, String sourceFile, String inputSchemaResource, String outputSchemaResource,
            String version, String functionDescription, String whenToUse, String whenNotToUse,
            String ioLimits, String displayDescription, String exampleQueries, String tags,
            String requiredEnvKeys, String ownerOrg, String definitionFile) {
    }
}
