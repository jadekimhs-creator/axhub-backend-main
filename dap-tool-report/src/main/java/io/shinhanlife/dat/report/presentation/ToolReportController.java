package io.shinhanlife.dat.report.presentation;

import io.shinhanlife.dat.report.application.ToolReportService;
import io.shinhanlife.dat.report.config.ReportProperties;
import io.shinhanlife.dat.report.model.ToolReportRequest;
import io.shinhanlife.dat.report.model.ToolSummary;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

/** 툴 선택 목록과 Excel 다운로드 API. */
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
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> excel(@RequestBody ToolReportRequest request) {
        byte[] content = service.createExcel(request.toolNames(), request.sourceType());
        String prefix = properties.outputFilenamePrefix() == null || properties.outputFilenamePrefix().isBlank()
                ? "tool-report" : properties.outputFilenamePrefix();
        String filename = prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xlsx";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(content.length)
                .body(content);
    }

    @GetMapping("/report-tools")
    public List<ToolSummary> tools(@RequestParam(value = "sourceType", required = false) String sourceType) {
        return service.findTools(sourceType);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
