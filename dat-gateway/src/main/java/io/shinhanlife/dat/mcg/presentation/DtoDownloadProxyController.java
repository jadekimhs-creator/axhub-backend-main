package io.shinhanlife.dat.mcg.presentation;

import io.shinhanlife.dat.mcg.config.GatewayFallbackProperties;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Aggregates DTO downloads from the CUS, SAL, PRO, and SYS Tool Pods. */
@RestController
public class DtoDownloadProxyController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final List<String> DTO_ROUTE_KEYS = List.of("cus", "sal", "pro", "sys");

    private final RestClient restClient;
    private final List<String> toolPodUrls;

    public DtoDownloadProxyController(
            RestClient.Builder restClientBuilder,
            GatewayFallbackProperties fallbackProperties) {
        this.restClient = restClientBuilder.build();
        this.toolPodUrls = resolveToolPodUrls(fallbackProperties);
    }

    @GetMapping("/dto-download/options")
    public List<String> options() {
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
    public ResponseEntity<byte[]> download(@PathVariable("dtoName") String dtoName) {
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
                .body(("DTO not found in CUS/SAL/PRO/SYS: " + dtoName)
                        .getBytes(StandardCharsets.UTF_8));
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
