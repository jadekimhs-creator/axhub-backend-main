package io.shinhanlife.dat.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @package io.shinhanlife.dat.report
 * @className ToolReportApplication
 * @description 툴 소스를 읽기 전용으로 분석하여 Excel 보고서를 생성하는 독립 애플리케이션
 * @author 0986406
 * @create 2026.08.07
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일       수정자     수정내용
 * ---------- -------- ---------------------------
 * 2026.08.07  0986406    최초생성
 * </pre>
 */
@SpringBootApplication
public class ToolReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToolReportApplication.class, args);
    }
}
