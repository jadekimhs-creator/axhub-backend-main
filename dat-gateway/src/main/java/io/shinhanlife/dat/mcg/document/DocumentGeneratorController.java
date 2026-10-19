package io.shinhanlife.dat.mcg.document;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.shinhanlife.dat.mcg.presentation.ToolReportProxyController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @package io.shinhanlife.dat.mcg.document
 * @className DocumentGeneratorController
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
@RequestMapping("/mcp/api/v1/admin/documents")
public class DocumentGeneratorController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final DocumentGeneratorService documentGeneratorService;
    private final ToolReportProxyController toolReportProxyController;

    @Autowired
    public DocumentGeneratorController(DocumentGeneratorService documentGeneratorService,
                                       @Autowired(required = false) ToolReportProxyController toolReportProxyController) {
        this.documentGeneratorService = documentGeneratorService;
        this.toolReportProxyController = toolReportProxyController;
    }

    public DocumentGeneratorController(DocumentGeneratorService documentGeneratorService) {
        this(documentGeneratorService, null);
    }

    @GetMapping("/tools")
    public List<Map<String, Object>> listTools(
            @RequestParam(value = "projectPath", required = false) String projectPath) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (toolReportProxyController != null) {
            try {
                List<ToolReportProxyController.ReportToolSummary> summaries = toolReportProxyController.tools(null, projectPath);
                if (summaries != null) {
                    for (ToolReportProxyController.ReportToolSummary s : summaries) {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("uid", s.name());
                        map.put("name", s.name());
                        map.put("displayName", s.title() == null || s.title().isBlank() ? s.name() : s.title());
                        map.put("description", s.description() == null ? "" : s.description());
                        map.put("categoryKey", s.categoryKey() == null ? "com" : s.categoryKey());
                        map.put("integrationType", "DIRECT");
                        map.put("mciServiceId", s.mappingId() == null ? "" : s.mappingId());
                        map.put("functionDescription", s.functionDescription() == null ? "" : s.functionDescription());
                        map.put("whenToUse", s.whenToUse() == null ? "" : s.whenToUse());
                        map.put("whenNotToUse", s.whenNotToUse() == null ? "" : s.whenNotToUse());
                        map.put("ioLimits", s.ioLimits() == null ? "" : s.ioLimits());
                        map.put("displayDescription", s.displayDescription() == null ? "" : s.displayDescription());
                        map.put("tags", s.tags() == null || s.tags().isBlank() ? List.of() : List.of(s.tags().split("\\s*\\|\\s*")));
                        map.put("exampleQueries", s.exampleQueries() == null || s.exampleQueries().isBlank() ? List.of() : List.of(s.exampleQueries().split("\\s*\\|\\s*")));
                        map.put("requiresApproval", s.requiresApproval());
                        map.put("readOnlyHint", s.readOnlyHint());
                        map.put("idempotentHint", s.idempotentHint());
                        map.put("destructiveHint", s.destructiveHint());
                        map.put("visible", true);
                        map.put("semver", s.version() == null || s.version().isBlank() ? "1.0.0" : s.version());
                        map.put("sourceFile", s.sourceFile() == null ? "" : s.sourceFile());

                        Map<String, Object> properties = new LinkedHashMap<>();
                        if (s.requestType() != null && !s.requestType().isBlank()) {
                            properties.put("request", Map.of(
                                    "type", "object",
                                    "description", (s.title() == null ? s.name() : s.title()) + " 요청 파라미터 (" + s.requestType() + ")"
                            ));
                        }
                        map.put("parametersSchema", Map.of("type", "object", "properties", properties));
                        map.put("outputSchema", Map.of("type", "object", "properties", Map.of(
                                "resultData", Map.of("type", "object", "description", (s.title() == null ? s.name() : s.title()) + " 응답 데이터 (" + (s.responseType() == null ? "Response" : s.responseType()) + ")")
                        )));
                        result.add(map);
                    }
                }
            } catch (Exception e) {
                // Ignore and return empty list on failure
            }
        }
        return result;
    }

    @PostMapping(value = "/program", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> generateProgram(@RequestBody DocumentGenerationRequest request) {
        return download(documentGeneratorService.generateProgram(request));
    }

    @PostMapping(value = "/interface", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> generateInterface(@RequestBody DocumentGenerationRequest request) {
        return download(documentGeneratorService.generateInterface(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    private ResponseEntity<byte[]> download(GeneratedDocument document) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(XLSX)
                .contentLength(document.content().length)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(document.content());
    }
}
