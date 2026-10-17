package io.shinhanlife.dat.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * @package io.shinhanlife.dat.report.config
 * @className ReportProperties
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
@ConfigurationProperties(prefix = "report")
public record ReportProperties(String dapWasDapmtSourceRoot,
                               String dapAdminSourceRoot,
                               String datWasDatpsSourceRoot,
                               String outputFilenamePrefix) {

    @ConstructorBinding
    public ReportProperties(String dapWasDapmtSourceRoot,
                            String dapAdminSourceRoot,
                            String datWasDatpsSourceRoot,
                            String outputFilenamePrefix) {
        this.dapWasDapmtSourceRoot = dapWasDapmtSourceRoot;
        this.dapAdminSourceRoot = dapAdminSourceRoot;
        this.datWasDatpsSourceRoot = datWasDatpsSourceRoot;
        this.outputFilenamePrefix = outputFilenamePrefix;
    }

    public ReportProperties(String dapWasDapmtSourceRoot, String dapAdminSourceRoot, String outputFilenamePrefix) {
        this(dapWasDapmtSourceRoot, dapAdminSourceRoot, null, outputFilenamePrefix);
    }
}
