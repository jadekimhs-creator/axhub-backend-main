package io.shinhanlife.dat.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 보고서 소스 위치와 출력 정책 설정. */
@ConfigurationProperties(prefix = "report")
public record ReportProperties(String dapWasDapmtSourceRoot,
                               String dapAdminSourceRoot,
                               String outputFilenamePrefix) {
}
