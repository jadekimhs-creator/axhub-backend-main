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

/**
 * @package io.shinhanlife.dat.report.presentation
 * @className ToolReportController
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
        byte[] content = service.createExcel(request.toolNames(), request.sourceType(), request.projectPath());
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
    public ResponseEntity<Map<String, String>> internalError(Exception exception) {
        return ResponseEntity.status(500).body(Map.of("error", exception.getMessage() == null ? "Internal server error" : exception.getMessage()));
    }
}
