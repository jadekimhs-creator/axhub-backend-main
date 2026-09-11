package io.shinhanlife.dat.report.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 보고서 모듈 설정. */
@Configuration
@EnableConfigurationProperties(ReportProperties.class)
public class ReportConfiguration {
}
