package io.shinhanlife.dat.mcg.presentation;

import io.shinhanlife.dat.mcg.config.GatewayFallbackProperties;
import io.shinhanlife.dat.mcg.document.MciDtoExcelService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Aggregates DTO downloads from configured Tool Pods or generates Excel directly from Workspace Path.
 */
@Slf4j
@RestController
public class DtoDownloadProxyController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final List<String> DTO_ROUTE_KEYS = List.of("cus", "sal", "pro", "sys");

    private final RestClient restClient;
    private final List<String> toolPodUrls;
    private final MciDtoExcelService mciDtoExcelService;

    public DtoDownloadProxyController(
            RestClient.Builder restClientBuilder,
            GatewayFallbackProperties fallbackProperties,
            MciDtoExcelService mciDtoExcelService) {
        this.restClient = restClientBuilder.build();
        this.toolPodUrls = resolveToolPodUrls(fallbackProperties);
        this.mciDtoExcelService = mciDtoExcelService;
    }

    @GetMapping("/dto-download/options")
    public List<String> options(
            @RequestParam(value = "workspacePath", required = false) String workspacePath,
            @RequestParam(value = "moduleName", required = false) String moduleName) {

        // 1. workspacePath가 지정된 경우 소스 파일 시스템에서 직접 스캔
        if (workspacePath != null && !workspacePath.isBlank()) {
            List<String> workspaceDtos = mciDtoExcelService.scanDtoNames(workspacePath, moduleName);
            if (!workspaceDtos.isEmpty()) {
                return workspaceDtos;
            }
        }

        // 2. 실행 중인 Tool Pod들로부터 DTO 목록 통합 수집 (기존 폴백)
        Set<String> mergedOptions = new LinkedHashSet<>();
        for (String toolPodUrl : toolPodUrls) {
            try {
                List<String> podOptions = restClient.get()
                        .uri(toolPodUrl + "/dto-download/options")
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
                if (podOptions != null) {
                    mergedOptions.addAll(podOptions);
                }
            } catch (RestClientException ignored) {
                // An unavailable Pod must not hide DTOs returned by the other running Pods.
            }
        }
        return mergedOptions.stream().sorted().toList();
    }

    @GetMapping("/dto-download/{dtoName}")
    public ResponseEntity<byte[]> download(
            @PathVariable("dtoName") String dtoName,
            @RequestParam(value = "workspacePath", required = false) String workspacePath,
            @RequestParam(value = "moduleName", required = false) String moduleName) {

        // 1. workspacePath가 지정된 경우 소스 파일에서 직접 엑셀 생성
        if (workspacePath != null && !workspacePath.isBlank()) {
            try {
                byte[] workbook = mciDtoExcelService.generateExcelWorkbook(workspacePath, moduleName, dtoName);
                return workbookResponse(dtoName, workbook);
            } catch (IOException e) {
                log.warn("Failed to generate workbook from workspace path: {} ({})", dtoName, e.getMessage());
            }
        }

        // 2. 실행 중인 Tool Pod에서 DTO 엑셀 다운로드 (기존 폴백)
        for (String toolPodUrl : toolPodUrls) {
            try {
                byte[] workbook = restClient.get()
                        .uri(toolPodUrl + "/dto-download/{dtoName}", dtoName)
                        .retrieve()
                        .body(byte[].class);
                return workbookResponse(dtoName, workbook);
            } catch (RestClientResponseException error) {
                if (error.getStatusCode().value() == 404) {
                    continue;
                }
                return downstreamError(error);
            } catch (RestClientException ignored) {
                // Try the next configured Tool Pod when this Pod cannot be reached.
            }
        }

        return ResponseEntity.status(404)
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(("DTO not found: " + dtoName).getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> resolveToolPodUrls(GatewayFallbackProperties properties) {
        List<String> urls = new ArrayList<>();
        for (String routeKey : DTO_ROUTE_KEYS) {
            String url = properties.getRoutes().get(routeKey);
            if (url != null && !url.isBlank()) {
                String normalized = url.replaceAll("/+$", "");
                if (!urls.contains(normalized)) {
                    urls.add(normalized);
                }
            }
        }
        if (urls.isEmpty() && properties.getDefaultUrl() != null
                && !properties.getDefaultUrl().isBlank()) {
            urls.add(properties.getDefaultUrl().replaceAll("/+$", ""));
        }
        return List.copyOf(urls);
    }

    private static ResponseEntity<byte[]> workbookResponse(String dtoName, byte[] workbook) {
        byte[] body = workbook == null ? new byte[0] : workbook;
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .contentLength(body.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(dtoName + ".xlsx").build().toString())
                .body(body);
    }

    private static ResponseEntity<byte[]> downstreamError(RestClientResponseException error) {
        return ResponseEntity.status(error.getStatusCode())
                .contentType(error.getResponseHeaders() != null
                        && error.getResponseHeaders().getContentType() != null
                        ? error.getResponseHeaders().getContentType() : MediaType.TEXT_PLAIN)
                .body(error.getResponseBodyAsByteArray());
    }
}
