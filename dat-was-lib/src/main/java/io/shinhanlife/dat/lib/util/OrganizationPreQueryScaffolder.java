package io.shinhanlife.dat.lib.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Generates a Pod-local MCI adapter for organization-number pre-queries. */
public final class OrganizationPreQueryScaffolder {

    private static final String BASE_PACKAGE = "io.shinhanlife.dat.mcc";
    private static final Path BASE_PACKAGE_PATH = Path.of("io", "shinhanlife", "dat", "mcc");

    private OrganizationPreQueryScaffolder() {
    }

    public record Definition(
            String moduleName,
            String adapterName,
            String interfaceId,
            String mciIoPrefix,
            String clientSystemCode,
            String requestFieldName,
            String responseOrganizationNoFieldName,
            String responseOrganizationNameFieldName,
            String pagingMode) {

        public Definition(
                String moduleName,
                String adapterName,
                String interfaceId,
                String mciIoPrefix,
                String clientSystemCode,
                String requestFieldName,
                String responseOrganizationNoFieldName,
                String responseOrganizationNameFieldName) {
            this(moduleName, adapterName, interfaceId, mciIoPrefix, clientSystemCode,
                 requestFieldName, responseOrganizationNoFieldName, responseOrganizationNameFieldName, "NONE");
        }
    }

    public static String scaffold(Definition definition) throws IOException {
        validate(definition);

        String sourceDirectory = System.getProperty("AXHUB_SOURCE_DIR");
        if (sourceDirectory == null || sourceDirectory.isBlank()) {
            throw new IllegalStateException("AXHUB_SOURCE_DIR is required");
        }

        String adapterName = pascalCase(definition.adapterName());
        String normalizedSystemCode = definition.clientSystemCode().trim().toUpperCase(Locale.ROOT);
        String mciPackageSegment = mciPackageSegment(normalizedSystemCode);
        String clientClassName = "Mci" + pascalCase(mciClientPrefix(normalizedSystemCode).toLowerCase(Locale.ROOT)) + "Client";
        String ioPrefix = definition.mciIoPrefix().trim().toUpperCase(Locale.ROOT);
        String adapterPackage = BASE_PACKAGE + ".common";
        String mciPackage = BASE_PACKAGE + ".infra.itrf.mci." + mciPackageSegment;
        String pagingMode = definition.pagingMode() != null ? definition.pagingMode().trim().toUpperCase(Locale.ROOT) : "NONE";

        Path rootDir = Path.of(sourceDirectory);
        Path moduleRoot = ToolScaffolder.resolveModuleRoot(rootDir, definition.moduleName().trim());
        Path sourceRoot = moduleRoot.resolve("src/main/java").resolve(BASE_PACKAGE_PATH);
        Path adapterDirectory = sourceRoot.resolve("common");
        Path implDirectory = adapterDirectory.resolve("impl");
        Files.createDirectories(implDirectory);

        boolean isNbsEmployeeLookup = "EmployeeLookup".equalsIgnoreCase(adapterName)
                && (ioPrefix.toUpperCase(Locale.ROOT).startsWith("ONBS")
                    || normalizedSystemCode.startsWith("ONBS")
                    || (definition.interfaceId() != null && definition.interfaceId().toUpperCase(Locale.ROOT).contains("NBS")));

        if (isNbsEmployeeLookup) {
            ensureEmployeeLookupComponents(sourceRoot);
            // Generate full EmployeeLookupAdapter with 2-step lookup, 3 paging modes, and candidate employee DTO
            Path dtoDir = sourceRoot.resolve("common/dto");
            Files.createDirectories(dtoDir);
            Path candidateDtoFile = dtoDir.resolve("CandidateEmployeeDto.java");
            if (!Files.exists(candidateDtoFile)) {
                writeNew(candidateDtoFile, candidateEmployeeDtoContent());
            }

            writeNew(adapterDirectory.resolve("EmployeeLookupAdapter.java"), employeeLookupInterfaceContent(adapterPackage));
            writeNew(implDirectory.resolve("EmployeeLookupAdapterImpl.java"), employeeLookupImplContent(adapterPackage));
            return "Generated organization pre-query adapter (EmployeeLookup with candidate employee DTO): " + adapterDirectory;
        }

        requireExistingMciContract(sourceRoot, mciPackageSegment, clientClassName, ioPrefix);

        String interfacePagingMethods = "";
        String implPagingMethods = "";
        String pagingImports = """
                import io.shinhanlife.glow.db.dto.AuditInfo;
                import io.shinhanlife.glow.db.dto.PageInfo;
                import io.shinhanlife.glow.db.dto.ScrPageInfo;
                """;

        if ("SCROLL".equals(pagingMode)) {
            interfacePagingMethods = """

                    %s_O findScroll(%s_I request, ScrPageInfo scrPageInfo);
                    """.formatted(ioPrefix, ioPrefix);
            implPagingMethods = """

                    @Override
                    public %s_O findScroll(%s_I request, ScrPageInfo scrPageInfo) {
                        if (request == null) {
                            throw new IllegalArgumentException("request is required");
                        }
                        return find(request);
                    }
                    """.formatted(ioPrefix, ioPrefix);
        } else if ("PAGE_NUMBER".equals(pagingMode)) {
            interfacePagingMethods = """

                    %s_O findPageNumber(%s_I request, PageInfo pageInfo);
                    """.formatted(ioPrefix, ioPrefix);
            implPagingMethods = """

                    @Override
                    public %s_O findPageNumber(%s_I request, PageInfo pageInfo) {
                        if (request == null) {
                            throw new IllegalArgumentException("request is required");
                        }
                        return find(request);
                    }
                    """.formatted(ioPrefix, ioPrefix);
        }

        writeNew(adapterDirectory.resolve(adapterName + "Adapter.java"), """
                package %s;

                import %s.io.%s_I;
                import %s.io.%s_O;
                %s
                /**
                 * @package %s
                 * @className %sAdapter
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
                public interface %sAdapter {

                    %s_O find(%s_I request);%s
                }
                """.formatted(adapterPackage,
                mciPackage, ioPrefix,
                mciPackage, ioPrefix,
                pagingImports,
                adapterPackage, adapterName,
                adapterName, ioPrefix, ioPrefix,
                interfacePagingMethods));

        String implPackage = adapterPackage + ".impl";

        writeNew(implDirectory.resolve(adapterName + "AdapterImpl.java"), """
                package %s;

                import %s.%sAdapter;
                import %s.%s;
                import %s.io.%s_I;
                import %s.io.%s_O;
                import io.shinhanlife.glow.communication.dto.Transfer;
                %s
                import lombok.RequiredArgsConstructor;
                import org.springframework.stereotype.Component;

                /**
                 * @package %s
                 * @className %sAdapterImpl
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
                @Component
                @RequiredArgsConstructor
                public class %sAdapterImpl implements %sAdapter {

                    private static final String INTERFACE_ID = "%s";
                    private static final String RECEIVE_SERVICE_ID = "%s";

                    private final %s mci;

                    @Override
                    public %s_O find(%s_I request) {
                        if (request == null) {
                            throw new IllegalArgumentException("request is required for organization lookup");
                        }

                        try {
                            Transfer<%s_O> transfer = mci.callTo(INTERFACE_ID, RECEIVE_SERVICE_ID, request, %s_O.class);
                            return transfer == null ? null : transfer.getBody();
                        } catch (RuntimeException exception) {
                            throw exception;
                        } catch (Exception exception) {
                            throw new IllegalStateException("Organization lookup MCI call failed", exception);
                        }
                    }%s
                }
                """.formatted(
                implPackage,
                adapterPackage, adapterName,
                mciPackage, clientClassName,
                mciPackage, ioPrefix,
                mciPackage, ioPrefix,
                pagingImports,
                implPackage, adapterName,
                adapterName, adapterName,
                definition.interfaceId().trim().toUpperCase(Locale.ROOT), normalizedSystemCode,
                clientClassName,
                ioPrefix, ioPrefix,
                ioPrefix, ioPrefix,
                implPagingMethods));

        return "Generated organization pre-query adapter: " + adapterDirectory;
    }

    private static void validate(Definition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition is required");
        }
        if (definition.moduleName() == null || !definition.moduleName().matches("[A-Za-z0-9][A-Za-z0-9-]*")) {
            throw new IllegalArgumentException("moduleName must contain letters, numbers, or hyphens");
        }
        requiredIdentifier(definition.adapterName(), "adapterName");
        requiredIdentifier(definition.interfaceId(), "interfaceId");
        requiredIdentifier(definition.mciIoPrefix(), "mciIoPrefix");
        requiredIdentifier(definition.requestFieldName(), "requestFieldName");
        requiredIdentifier(definition.responseOrganizationNoFieldName(), "responseOrganizationNoFieldName");
        requiredIdentifier(definition.responseOrganizationNameFieldName(), "responseOrganizationNameFieldName");
        String systemCode = definition.clientSystemCode() == null ? "" : definition.clientSystemCode().trim();
        if (systemCode.length() != 9) {
            throw new IllegalArgumentException("clientSystemCode must contain exactly 9 characters");
        }
    }

    private static void requiredIdentifier(String value, String label) {
        if (value == null || !value.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(label + " must be a Java identifier");
        }
    }

    private static String mciPackageSegment(String clientSystemCode) {
        if (clientSystemCode.length() == 9) {
            return clientSystemCode.substring(1, 4).toLowerCase(Locale.ROOT)
                    + "." + clientSystemCode.substring(4, 5).toLowerCase(Locale.ROOT);
        }
        return clientSystemCode.substring(0, 3).toLowerCase(Locale.ROOT)
                + "." + clientSystemCode.substring(3).toLowerCase(Locale.ROOT);
    }

    private static String mciClientPrefix(String clientSystemCode) {
        return clientSystemCode.length() == 9 ? clientSystemCode.substring(1, 5) : clientSystemCode;
    }

    private static String pascalCase(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static void writeNew(Path file, String content) throws IOException {
        if (Files.exists(file)) {
            throw new IllegalStateException("Refusing to overwrite existing adapter file: " + file);
        }
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static void requireExistingMciContract(Path sourceRoot, String mciPackageSegment,
                                                   String clientClassName, String ioPrefix) {
        Path mciDirectory = sourceRoot.resolve("infra/itrf/mci")
                .resolve(mciPackageSegment.replace('.', '/'));
        Path client = mciDirectory.resolve(clientClassName + ".java");
        Path request = mciDirectory.resolve("io").resolve(ioPrefix + "_I.java");
        Path response = mciDirectory.resolve("io").resolve(ioPrefix + "_O.java");
        if (!Files.isRegularFile(client) || !Files.isRegularFile(request) || !Files.isRegularFile(response)) {
            throw new IllegalStateException("MCI contract is missing. Generate/select "
                    + ioPrefix + "_I, " + ioPrefix + "_O, and " + clientClassName + " first.");
        }
    }

    private static void ensureEmployeeLookupComponents(Path sourceRoot) throws IOException {
        if (sourceRoot == null) return;
        Path commonDir = sourceRoot.resolve("common");
        Path commonImplDir = commonDir.resolve("impl");
        Path commonDtoDir = commonDir.resolve("dto");
        Path mciNbszDir = sourceRoot.resolve("infra/itrf/mci/nbs/z");
        Path mciNbszIoDir = mciNbszDir.resolve("io");
        Path mciNbsaDir = sourceRoot.resolve("infra/itrf/mci/nbs/a");
        Path mciNbsaIoDir = mciNbsaDir.resolve("io");

        Files.createDirectories(commonDir);
        Files.createDirectories(commonImplDir);
        Files.createDirectories(commonDtoDir);
        Files.createDirectories(mciNbszDir);
        Files.createDirectories(mciNbszIoDir);
        Files.createDirectories(mciNbsaDir);
        Files.createDirectories(mciNbsaIoDir);

        Path nbszClientFile = mciNbszDir.resolve("MciNbszClient.java");
        if (!Files.exists(nbszClientFile)) {
            Files.writeString(nbszClientFile, mciNbszClientContent(), StandardCharsets.UTF_8);
        }

        Path onbsz0460IFile = mciNbszIoDir.resolve("ONBSZ0460_I.java");
        if (!Files.exists(onbsz0460IFile)) {
            Files.writeString(onbsz0460IFile, onbsz0460IContent(), StandardCharsets.UTF_8);
        }

        Path onbsz0460OFile = mciNbszIoDir.resolve("ONBSZ0460_O.java");
        if (!Files.exists(onbsz0460OFile)) {
            Files.writeString(onbsz0460OFile, onbsz0460OContent(), StandardCharsets.UTF_8);
        }

        Path nbsaClientFile = mciNbsaDir.resolve("MciNbsaClient.java");
        if (!Files.exists(nbsaClientFile)) {
            Files.writeString(nbsaClientFile, mciNbsaClientContent(), StandardCharsets.UTF_8);
        }

        Path onbsa1030IFile = mciNbsaIoDir.resolve("ONBSA1030_I.java");
        if (!Files.exists(onbsa1030IFile)) {
            Files.writeString(onbsa1030IFile, onbsa1030IContent(), StandardCharsets.UTF_8);
        }

        Path onbsa1030OFile = mciNbsaIoDir.resolve("ONBSA1030_O.java");
        if (!Files.exists(onbsa1030OFile)) {
            Files.writeString(onbsa1030OFile, onbsa1030OContent(), StandardCharsets.UTF_8);
        }
    }

    private static String mciNbszClientContent() {
        return """
package io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.z;

import io.shinhanlife.dat.lib.integration.mci.component.AxhubMciComponent;
import io.shinhanlife.glow.communication.dto.Transfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @package io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.z
 * @className MciNbszClient
 * @description 인사정보 MCI 클라이언트 (DATNBSO00005 연동)
 */
@Component
@RequiredArgsConstructor
public class MciNbszClient {

    private final AxhubMciComponent mciComponent;

    public <O> Transfer<O> callTo(String interfaceId, String receiveServiceId, Object mciReq, Class<O> resType) {
        return mciComponent.callTo(interfaceId, receiveServiceId, mciReq, resType);
    }
}
""";
    }

    private static String onbsz0460IContent() {
        return """
package io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.z.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.shinhanlife.glow.db.dto.AuditInfo;
import io.shinhanlife.glow.db.dto.PageInfo;
import io.shinhanlife.glow.db.dto.ScrPageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ONBSZ0460_I {

    @Schema(description = "인사번호(사번)", example = "99999999")
    private String prafNo;

    @Schema(description = "인사명(성명)", example = "홍길동")
    private String prafNm;

    @Schema(description = "페이지정보")
    private PageInfo pageInfo;

    @Schema(description = "스크롤페이지정보")
    private ScrPageInfo scrPageInfo;

    @Schema(description = "감사정보")
    private AuditInfo auditInfo;

    public static ONBSZ0460_I of(String prafNo) {
        return ONBSZ0460_I.builder()
                .prafNo(prafNo)
                .build();
    }
}
""";
    }

    private static String onbsz0460OContent() {
        return """
package io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.z.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.shinhanlife.glow.db.dto.AuditInfo;
import io.shinhanlife.glow.db.dto.PageInfo;
import io.shinhanlife.glow.db.dto.ScrPageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ONBSZ0460_O {

    @Schema(description = "인사번호(사번)", example = "99999999")
    private String prafNo;

    @Schema(description = "인사명(성명)", example = "홍길동")
    private String prafNm;

    @Schema(description = "소속조직번호 (조직번호)", example = "005930")
    private String blngOgnzNo;

    @Schema(description = "소속조직명 (조직명)", example = "강남금융센터")
    private String blngOgnzNm;

    @Schema(description = "조직번호 (호환용)")
    private String ognzNo;

    @Schema(description = "조직명 (호환용)")
    private String ognzNm;

    @Schema(description = "페이지정보")
    private PageInfo pageInfo;

    @Schema(description = "스크롤페이지정보")
    private ScrPageInfo scrPageInfo;

    @Schema(description = "감사정보")
    private AuditInfo auditInfo;

    public String getEffectiveOrganizationNo() {
        if (blngOgnzNo != null && !blngOgnzNo.isBlank()) {
            return blngOgnzNo;
        }
        if (ognzNo != null && !ognzNo.isBlank()) {
            return ognzNo;
        }
        return null;
    }

    public String getEffectiveOrganizationName() {
        if (blngOgnzNm != null && !blngOgnzNm.isBlank()) {
            return blngOgnzNm;
        }
        if (ognzNm != null && !ognzNm.isBlank()) {
            return ognzNm;
        }
        return null;
    }
}
""";
    }

    private static String mciNbsaClientContent() {
        return """
package io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.a;

import io.shinhanlife.dat.lib.integration.mci.component.AxhubMciComponent;
import io.shinhanlife.glow.communication.dto.Transfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @package io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.a
 * @className MciNbsaClient
 * @description 인사검색 목록조회 MCI 클라이언트 (DATNBSO00004 연동)
 */
@Component
@RequiredArgsConstructor
public class MciNbsaClient {

    private final AxhubMciComponent mciComponent;

    public <O> Transfer<O> callTo(String interfaceId, String receiveServiceId, Object mciReq, Class<O> resType) {
        return mciComponent.callTo(interfaceId, receiveServiceId, mciReq, resType);
    }
}
""";
    }

    private static String onbsa1030IContent() {
        return """
package io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.a.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.shinhanlife.glow.GlowTrgmField;
import io.shinhanlife.glow.db.dto.PageInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * ONBSA1030_I 메인 DTO
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ONBSA1030_I {

    @GlowTrgmField(order = 1, description = "인사검색목록조회InDto", type = "gm")
    private List<PrafSrchListInqrInDto> prafSrchListInqrInDto;

    @GlowTrgmField(order = 2, description = "페이지정보", type = "gm")
    private List<PageInfo> pageInfo;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class PrafSrchListInqrInDto {

        @GlowTrgmField(order = 1, length = 50, description = "조회조건값1")
        private String inqrCndtValu1;

        @GlowTrgmField(order = 2, length = 50, description = "조회조건값2")
        private String inqrCndtValu2;

        @GlowTrgmField(order = 3, length = 50, description = "조회조건값3")
        private String inqrCndtValu3;
    }
}
""";
    }

    private static String onbsa1030OContent() {
        return """
package io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.a.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.shinhanlife.glow.GlowTrgmField;
import io.shinhanlife.glow.db.dto.PageInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * ONBSA1030_O 메인 DTO
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ONBSA1030_O {

    @GlowTrgmField(order = 1, length = 5, description = "인사검색목록조회OutDto_cnt", target = "prafSrchListInqrOutDto")
    private int prafSrchListInqrOutDto_cnt;

    @GlowTrgmField(order = 2, description = "인사검색목록조회OutDto", type = "gm")
    private List<PrafSrchListInqrOutDto> prafSrchListInqrOutDto;

    @GlowTrgmField(order = 3, description = "페이지정보", type = "gm")
    private List<PageInfo> pageInfo;

    public List<PrafSrchListInqrOutDto> getResults() {
        if (prafSrchListInqrOutDto != null && !prafSrchListInqrOutDto.isEmpty()) {
            return prafSrchListInqrOutDto;
        }
        return Collections.emptyList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class PrafSrchListInqrOutDto {

        @GlowTrgmField(order = 1, length = 8, description = "인사번호")
        private String prafNo;

        @GlowTrgmField(order = 2, length = 2, description = "인사유형코드")
        private String prafTypeCd;

        @GlowTrgmField(order = 3, length = 200, description = "인사명")
        private String prafNm;

        @GlowTrgmField(order = 4, length = 100, description = "인사영문명")
        private String prafEngNm;

        @GlowTrgmField(order = 5, length = 8, description = "생년월일")
        private String birymd;

        @GlowTrgmField(order = 6, length = 1, description = "성별코드")
        private String gndrCd;

        @GlowTrgmField(order = 7, length = 13, description = "주민등록번호")
        private String rdreNo;

        @GlowTrgmField(order = 8, length = 7, description = "소속조직번호")
        private String blngOgnzNo;

        @GlowTrgmField(order = 9, length = 7, description = "조직번호")
        private String ognzNo;

        @GlowTrgmField(order = 10, length = 100, description = "조직명")
        private String ognzNm;

        @GlowTrgmField(order = 11, length = 3, description = "조직분류코드")
        private String ognzAsrtCd;

        @GlowTrgmField(order = 12, length = 3, description = "인사조직분류코드")
        private String psmrAsrtCd;

        @GlowTrgmField(order = 13, length = 2, description = "영업규정분류코드")
        private String sbsnRulpAsrtCd;

        @GlowTrgmField(order = 14, length = 2, description = "조직레벨코드")
        private String ognzLeveCd;

        @GlowTrgmField(order = 15, length = 7, description = "최하위소속조직번호")
        private String lrnkBlngOgnzNo;

        @GlowTrgmField(order = 16, length = 4, description = "지점번호")
        private String brafNo;

        @GlowTrgmField(order = 17, length = 4, description = "영업소코드")
        private String bsquCd;

        @GlowTrgmField(order = 18, length = 3, description = "영업직책코드")
        private String bsduCd;

        @GlowTrgmField(order = 19, length = 2, description = "위촉구분코드")
        private String appnScCd;

        @GlowTrgmField(order = 20, length = 2, description = "위촉경력구분코드")
        private String appnCareScCd;

        @GlowTrgmField(order = 21, length = 2, description = "위해촉최종상태코드")
        private String aprdLsstCd;

        @GlowTrgmField(order = 22, length = 8, description = "위촉일자")
        private String appnYmd;

        @GlowTrgmField(order = 23, length = 8, description = "부임일자")
        private String acsoYmd;

        public String getEffectiveOrganizationNo() {
            if (blngOgnzNo != null && !blngOgnzNo.isBlank()) {
                return blngOgnzNo;
            }
            if (lrnkBlngOgnzNo != null && !lrnkBlngOgnzNo.isBlank()) {
                return lrnkBlngOgnzNo;
            }
            if (ognzNo != null && !ognzNo.isBlank()) {
                return ognzNo;
            }
            if (brafNo != null && !brafNo.isBlank()) {
                return brafNo;
            }
            if (ognzAsrtCd != null && !ognzAsrtCd.isBlank()) {
                return ognzAsrtCd;
            }
            return null;
        }
    }
}
""";
    }

    private static String candidateEmployeeDtoContent() {
        return """
package io.shinhanlife.dat.mcc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateEmployeeDto {

    @Schema(description = "인사번호(사번)", example = "20240001")
    private String employeeNo;

    @Schema(description = "인사명(성명)", example = "홍길동")
    private String employeeName;

    @Schema(description = "조직/부서 번호", example = "005930")
    private String organizationNo;

    @Schema(description = "소속 조직명", example = "강남금융센터")
    private String organizationName;
}
""";
    }

    private static String employeeLookupInterfaceContent(String adapterPackage) {
        return """
package %s;

import io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.a.io.ONBSA1030_I;
import io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.a.io.ONBSA1030_O;
import io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.z.io.ONBSZ0460_I;
import io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.z.io.ONBSZ0460_O;
import io.shinhanlife.glow.db.dto.AuditInfo;
import io.shinhanlife.glow.db.dto.PageInfo;
import io.shinhanlife.glow.db.dto.ScrPageInfo;

import java.util.List;

public interface EmployeeLookupAdapter {

    String getHeaderEmployeeNo();

    ONBSZ0460_O.CmmnPrafIfinOutDto findCommonEmployee(String employeeNo);

    ONBSZ0460_O findCommonEmployee(ONBSZ0460_I request);

    List<ONBSA1030_O.PrafSrchListInqrOutDto> searchEmployeeList(String employeeNo, String employeeName);

    List<ONBSA1030_O.PrafSrchListInqrOutDto> searchEmployeeListPageNumber(String employeeNo, String employeeName, PageInfo pageInfo);

    List<ONBSA1030_O.PrafSrchListInqrOutDto> searchEmployeeListScroll(String employeeNo, String employeeName, ScrPageInfo scrPageInfo);

    List<ONBSA1030_O.PrafSrchListInqrOutDto> searchEmployeeList(ONBSA1030_I request);

    ONBSA1030_O searchEmployee(ONBSA1030_I request);
}
""".formatted(adapterPackage);
    }

    private static String employeeLookupImplContent(String adapterPackage) {
        return """
package %s.impl;

import io.shinhanlife.dat.lib.mcp.McpRequestHeaderContext;
import io.shinhanlife.dat.lib.mcp.McpRequestHeaders;
import io.shinhanlife.dat.lib.util.SessionUtil;
import io.shinhanlife.dat.mcc.common.EmployeeLookupAdapter;
import io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.a.MciNbsaClient;
import io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.a.io.ONBSA1030_I;
import io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.a.io.ONBSA1030_O;
import io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.z.MciNbszClient;
import io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.z.io.ONBSZ0460_I;
import io.shinhanlife.dat.mcc.infra.itrf.mci.nbs.z.io.ONBSZ0460_O;
import io.shinhanlife.glow.communication.dto.Transfer;
import io.shinhanlife.glow.db.dto.AuditInfo;
import io.shinhanlife.glow.db.dto.PageInfo;
import io.shinhanlife.glow.db.dto.ScrPageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeLookupAdapterImpl implements EmployeeLookupAdapter {

    private final MciNbszClient mciNbszClient;
    private final MciNbsaClient mciNbsaClient;

    @Override
    public String getHeaderEmployeeNo() {
        McpRequestHeaders headers = McpRequestHeaderContext.current();
        if (headers != null && headers.prafNo() != null && !headers.prafNo().isBlank()) {
            return headers.prafNo().trim();
        }
        String prafNo = SessionUtil.getPrafNo();
        return (prafNo != null && !prafNo.isBlank()) ? prafNo.trim() : null;
    }

    @Override
    public ONBSZ0460_O.CmmnPrafIfinOutDto findCommonEmployee(String employeeNo) {
        if (employeeNo == null || employeeNo.isBlank()) {
            log.warn("[EmployeeLookupAdapter] 사번(employeeNo)이 누락되어 공통인사정보조회를 수행할 수 없습니다.");
            return null;
        }

        ONBSZ0460_I.CmmnPrafIfinInDto inDto = ONBSZ0460_I.CmmnPrafIfinInDto.builder()
                .prafNo(employeeNo.trim())
                .build();

        ONBSZ0460_I request = ONBSZ0460_I.builder()
                .cmmnPrafIfinInDto(List.of(inDto))
                .build();

        ONBSZ0460_O response = findCommonEmployee(request);
        if (response != null && response.getCmmnPrafIfinOutDto() != null && !response.getCmmnPrafIfinOutDto().isEmpty()) {
            return response.getCmmnPrafIfinOutDto().get(0);
        }
        return null;
    }

    @Override
    public ONBSZ0460_O findCommonEmployee(ONBSZ0460_I request) {
        if (request == null) {
            log.warn("[EmployeeLookupAdapter] 공통인사정보조회 요청 DTO가 null입니다.");
            return null;
        }

        try {
            Transfer<ONBSZ0460_O> transfer = mciNbszClient.callTo(request);
            if (transfer != null && transfer.getBody() != null) {
                log.info("[EmployeeLookupAdapter] 공통인사정보조회(ONBSZ0460) MCI 연동 성공");
                return transfer.getBody();
            }
        } catch (Exception e) {
            log.error("[EmployeeLookupAdapter] 공통인사정보조회(ONBSZ0460) MCI 호출 중 오류 발생: msg={}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<ONBSA1030_O.PrafSrchListInqrOutDto> searchEmployeeList(String employeeNo, String employeeName) {
        PageInfo pi = new PageInfo(0, 20);
        pi.setPageNo(1);
        pi.setPageDataCc(20);
        return searchEmployeeListPageNumber(employeeNo, employeeName, pi);
    }

    @Override
    public List<ONBSA1030_O.PrafSrchListInqrOutDto> searchEmployeeListPageNumber(String employeeNo, String employeeName, PageInfo pageInfo) {
        if ((employeeNo == null || employeeNo.isBlank()) && (employeeName == null || employeeName.isBlank())) {
            log.warn("[EmployeeLookupAdapter] 인사검색 조건(사번 또는 성명)이 누락되었습니다.");
            return Collections.emptyList();
        }

        String val1 = (employeeNo != null && !employeeNo.isBlank()) ? employeeNo.trim() : (employeeName != null ? employeeName.trim() : "");
        String val2 = (employeeNo != null && !employeeNo.isBlank() && employeeName != null && !employeeName.isBlank()) ? employeeName.trim() : "";

        ONBSA1030_I.PrafSrchListInqrInDto inDto = ONBSA1030_I.PrafSrchListInqrInDto.builder()
                .inqrCndtValu1(val1)
                .inqrCndtValu2(val2)
                .inqrCndtValu3("")
                .build();

        PageInfo pi = pageInfo != null ? pageInfo : new PageInfo(0, 20);
        if (pageInfo == null) {
            pi.setPageNo(1);
            pi.setPageDataCc(20);
        }

        ONBSA1030_I request = ONBSA1030_I.builder()
                .prafSrchListInqrInDto(List.of(inDto))
                .pageInfo(List.of(pi))
                .build();

        return searchEmployeeList(request);
    }

    @Override
    public List<ONBSA1030_O.PrafSrchListInqrOutDto> searchEmployeeListScroll(String employeeNo, String employeeName, ScrPageInfo scrPageInfo) {
        if ((employeeNo == null || employeeNo.isBlank()) && (employeeName == null || employeeName.isBlank())) {
            log.warn("[EmployeeLookupAdapter] 인사검색 조건(사번 또는 성명)이 누락되었습니다.");
            return Collections.emptyList();
        }

        String val1 = (employeeNo != null && !employeeNo.isBlank()) ? employeeNo.trim() : (employeeName != null ? employeeName.trim() : "");
        String val2 = (employeeNo != null && !employeeNo.isBlank() && employeeName != null && !employeeName.isBlank()) ? employeeName.trim() : "";

        ONBSA1030_I.PrafSrchListInqrInDto inDto = ONBSA1030_I.PrafSrchListInqrInDto.builder()
                .inqrCndtValu1(val1)
                .inqrCndtValu2(val2)
                .inqrCndtValu3("")
                .build();

        int pageSize = (scrPageInfo != null && scrPageInfo.getPageDataCc() > 0) ? scrPageInfo.getPageDataCc() : 20;
        PageInfo pi = new PageInfo(0, pageSize);
        pi.setPageNo(1);
        pi.setPageDataCc(pageSize);

        ONBSA1030_I request = ONBSA1030_I.builder()
                .prafSrchListInqrInDto(List.of(inDto))
                .pageInfo(List.of(pi))
                .build();

        return searchEmployeeList(request);
    }

    @Override
    public ONBSA1030_O searchEmployee(ONBSA1030_I request) {
        if (request == null) {
            log.warn("[EmployeeLookupAdapter] 인사검색 요청 DTO가 null입니다.");
            return null;
        }

        if (request.getPageInfo() == null || request.getPageInfo().isEmpty()) {
            PageInfo pi = new PageInfo(0, 20);
            pi.setPageNo(1);
            pi.setPageDataCc(20);
            request.setPageInfo(List.of(pi));
        }

        try {
            Transfer<ONBSA1030_O> transfer = mciNbsaClient.callTo(request);
            if (transfer != null && transfer.getBody() != null) {
                log.info("[EmployeeLookupAdapter] 인사검색목록조회(ONBSA1030) MCI 연동 성공");
                return transfer.getBody();
            }
        } catch (Exception e) {
            log.error("[EmployeeLookupAdapter] 인사검색목록조회(ONBSA1030) MCI 호출 중 오류 발생: msg={}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<ONBSA1030_O.PrafSrchListInqrOutDto> searchEmployeeList(ONBSA1030_I request) {
        ONBSA1030_O response = searchEmployee(request);
        if (response != null) {
            List<ONBSA1030_O.PrafSrchListInqrOutDto> results = response.getResults();
            if (results != null && !results.isEmpty()) {
                log.info("[EmployeeLookupAdapter] 인사검색목록조회(ONBSA1030) 결과 {}건 수신", results.size());
                return results;
            }
        }
        return Collections.emptyList();
    }
}
""".formatted(adapterPackage);
    }
}
