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

/** Tool Report 화면과 소스 분석 서비스를 연결한다. */
@RestController
public class ToolReportProxyController {

    private final RestClient restClient;
    private final String reportServiceUrl;

    public ToolReportProxyController(RestClient.Builder restClientBuilder,
                                     @Value("${report.service-url}") String reportServiceUrl) {
        this.restClient = restClientBuilder.build();
        this.reportServiceUrl = reportServiceUrl.replaceAll("/+$", "");
    }

    @GetMapping(value = "/report/api/report-tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ReportToolSummary> tools(@RequestParam(value = "sourceType", required = false) String sourceType) {
        ReportToolSummary[] tools = restClient.get()
                .uri(reportServiceUrl + "/api/report-tools?sourceType={sourceType}",
                        sourceType == null ? "DAP_WAS_DAPMT" : sourceType)
                .retrieve()
                .body(ReportToolSummary[].class);
        return tools == null ? List.of() : List.of(tools);
    }

    @PostMapping(value = "/report/api/tool-reports/excel",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> excel(@RequestBody Map<String, Object> request) {
        ResponseEntity<byte[]> response = restClient.post()
                .uri(reportServiceUrl + "/api/tool-reports/excel")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(byte[].class);
        return copy(response);
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
