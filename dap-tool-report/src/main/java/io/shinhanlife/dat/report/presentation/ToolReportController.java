package io.shinhanlife.dat.report.presentation;

import io.shinhanlife.dat.report.application.ToolReportService;
import io.shinhanlife.dat.report.application.ToolReportService.ReportExportResult;
import io.shinhanlife.dat.report.config.ReportProperties;
import io.shinhanlife.dat.report.model.ToolReportRequest;
import io.shinhanlife.dat.report.model.ToolSummary;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tool 보고서 생성 및 다운로드 API 컨트롤러.
 */
@RestController
@RequestMapping("/api")
public class ToolReportController {

    private final ToolReportService service;
    private final ReportProperties properties;

    public ToolReportController(ToolReportService service, ReportProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @PostMapping(value = "/tool-reports/excel",
            produces = {
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/zip",
                    "application/octet-stream"
            })
    public ResponseEntity<byte[]> excel(@RequestBody ToolReportRequest request) {
        ReportExportResult result = service.exportReports(request.toolNames(), request.sourceType(), request.projectPath());
        String rawName = result.filename();
        String encodedName = java.net.URLEncoder.encode(rawName, StandardCharsets.UTF_8).replace("+", "%20");
        String asciiFallback = rawName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String disposition = "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encodedName;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.content().length)
                .body(result.content());
    }

    @GetMapping("/report-tools")
    public List<ToolSummary> tools(
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "projectPath", required = false) String projectPath) {
        return service.findTools(sourceType, projectPath);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> failure(Exception exception) {
        return ResponseEntity.internalServerError().body(Map.of("error", exception.getMessage()));
    }
}
