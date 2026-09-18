package io.shinhanlife.dat.lib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MciResponseScaffolderTest {

    private static final String MCI_SOURCE = """
            package io.shinhanlife.dat.mcc.infra.itrf.mci.onbsz.io;

            import io.shinhanlife.glow.communication.annotation.GlowTrgmField;
            import java.util.List;
            import lombok.Data;

            @Data
            public class ONBSZ0460_O {
                @GlowTrgmField(order = 1, length = 20, description = "인사정보 목록")
                private List<CmnnPrafIfinOutDto> employeeItems;

                @Data
                public static class CmnnPrafIfinOutDto {
                    @GlowTrgmField(order = 1, length = 8, description = "인사번호")
                    private String prafNo;

                    @GlowTrgmField(order = 2, length = 2, description = "인사유형코드")
                    private String prafTypeCd;

                    @GlowTrgmField(order = 3, length = 200, description = "인사명")
                    private String prafNm;

                    @GlowTrgmField(order = 4, length = 50, description = "주민등록번호")
                    private String rdreNo;
                }
            }
            """;

    @Test
    void parsesGlowDescriptionsAndNestedResponseFields() {
        MciResponseScaffolder.ParsedSource parsed = MciResponseScaffolder.parse(MCI_SOURCE);

        assertEquals("io.shinhanlife.dat.mcc.infra.itrf.mci.onbsz.io", parsed.packageName());
        assertEquals("ONBSZ0460_O", parsed.rootClassName());
        assertEquals(List.of("ONBSZ0460_O", "CmnnPrafIfinOutDto"),
                parsed.types().stream().map(MciResponseScaffolder.ParsedType::name).toList());
        assertEquals("인사번호", parsed.types().get(1).fields().getFirst().description());
        assertEquals(8, parsed.types().get(1).fields().getFirst().length());
        assertTrue(parsed.types().get(1).fields().get(3).sensitive());
    }

    @Test
    void generatesLlmResponseAndExplicitMapStructMappings() {
        MciResponseScaffolder.ParsedSource parsed = MciResponseScaffolder.parse(MCI_SOURCE);
        List<MciResponseScaffolder.FieldMapping> mappings = List.of(
                new MciResponseScaffolder.FieldMapping("ONBSZ0460_O", "employeeItems", "employees", true),
                new MciResponseScaffolder.FieldMapping("CmnnPrafIfinOutDto", "prafNo", "employeeNumber", true),
                new MciResponseScaffolder.FieldMapping("CmnnPrafIfinOutDto", "prafTypeCd", "employeeTypeCode", true),
                new MciResponseScaffolder.FieldMapping("CmnnPrafIfinOutDto", "prafNm", "employeeName", true),
                new MciResponseScaffolder.FieldMapping("CmnnPrafIfinOutDto", "rdreNo", "residentRegistrationNumber", true));

        MciResponseScaffolder.GeneratedSources generated = MciResponseScaffolder.generate(
                parsed,
                "io.shinhanlife.dat.mcc.biz.pro.dto",
                "IndividualCustomerDetailInquiryResponse",
                "io.shinhanlife.dat.mcc.biz.pro.converter",
                "IndividualCustomerDetailInquiryConverter",
                mappings);

        assertTrue(generated.responseSource().contains("@Schema(description = \"인사번호\")"));
        assertTrue(generated.responseSource().contains("private String employeeNumber;"));
        assertTrue(generated.responseSource().contains("private List<CmnnPrafIfinOutDto> employees;"));
        assertTrue(generated.responseSource().contains("public static class CmnnPrafIfinOutDto"));
        assertTrue(generated.converterSource().contains(
                "@Mapping(source = \"prafNo\", target = \"employeeNumber\")"));
        assertTrue(generated.converterSource().contains(
                "IndividualCustomerDetailInquiryResponse toResponse(ONBSZ0460_O source);"));
        assertTrue(generated.converterSource().contains(
                "IndividualCustomerDetailInquiryResponse.CmnnPrafIfinOutDto toCmnnPrafIfinOutDto(ONBSZ0460_O.CmnnPrafIfinOutDto source);"));
    }

    @Test
    void generatesLlmRequestAndMapsLlmFieldsBackToLegacyInput() {
        String requestSource = MCI_SOURCE.replace("ONBSZ0460_O", "ONBSZ0460_I");
        MciResponseScaffolder.ParsedSource parsed = MciResponseScaffolder.parse(requestSource);
        List<MciResponseScaffolder.FieldMapping> mappings = List.of(
                new MciResponseScaffolder.FieldMapping("ONBSZ0460_I", "employeeItems", "employees", true),
                new MciResponseScaffolder.FieldMapping("CmnnPrafIfinOutDto", "prafNo", "employeeNumber", true),
                new MciResponseScaffolder.FieldMapping("CmnnPrafIfinOutDto", "prafTypeCd", "employeeTypeCode", true),
                new MciResponseScaffolder.FieldMapping("CmnnPrafIfinOutDto", "prafNm", "employeeName", true),
                new MciResponseScaffolder.FieldMapping("CmnnPrafIfinOutDto", "rdreNo", "residentRegistrationNumber", true));

        MciResponseScaffolder.GeneratedRequestSources generated = MciResponseScaffolder.generateRequest(
                parsed,
                "io.shinhanlife.dat.mcc.biz.pro.dto",
                "IndividualCustomerDetailInquiryRequest",
                "io.shinhanlife.dat.mcc.biz.pro.converter",
                "IndividualCustomerDetailInquiryRequestConverter",
                mappings);

        assertTrue(generated.requestSource().contains("description = \"인사번호\""));
        assertTrue(generated.requestSource().contains("private String employeeNumber;"));
        assertTrue(generated.converterSource().contains(
                "@Mapping(source = \"employeeNumber\", target = \"prafNo\")"));
        assertTrue(generated.converterSource().contains(
                "ONBSZ0460_I toRequest(IndividualCustomerDetailInquiryRequest source);"));
        assertTrue(generated.converterSource().contains(
                "ONBSZ0460_I.CmnnPrafIfinOutDto toCmnnPrafIfinOutDto("));
    }

    @Test
    void rejectsDuplicateAiTargetNamesWithinTheSameType() {
        MciResponseScaffolder.ParsedSource parsed = MciResponseScaffolder.parse(MCI_SOURCE);
        List<MciResponseScaffolder.FieldMapping> mappings = List.of(
                new MciResponseScaffolder.FieldMapping("CmnnPrafIfinOutDto", "prafNo", "employeeNumber", true),
                new MciResponseScaffolder.FieldMapping("CmnnPrafIfinOutDto", "prafTypeCd", "employeeNumber", true));

        try {
            MciResponseScaffolder.generate(parsed, "io.example.dto", "EmployeeResponse",
                    "io.example.converter", "EmployeeConverter", mappings);
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().contains("중복"));
            return;
        }
        throw new AssertionError("Duplicate target names must be rejected");
    }

    @Test
    void preservesScrPageInfoAndPageInfoWithoutRenaming() {
        String sourceWithPaging = """
                package io.shinhanlife.dat.mcc.infra.itrf.mci.onbsz.io;

                import io.shinhanlife.glow.communication.annotation.GlowTrgmField;
                import io.shinhanlife.glow.db.dto.PageInfo;
                import io.shinhanlife.glow.db.dto.ScrPageInfo;
                import lombok.Data;

                @Data
                public class ONBSZ0460_O {
                    @GlowTrgmField(order = 1, length = 20, description = "페이지정보")
                    private PageInfo pageInfo;

                    @GlowTrgmField(order = 2, length = 20, description = "스크롤페이지정보")
                    private ScrPageInfo scrPageInfo;
                }
                """;
        MciResponseScaffolder.ParsedSource parsed = MciResponseScaffolder.parse(sourceWithPaging);
        List<MciResponseScaffolder.FieldMapping> mappings = List.of(
                new MciResponseScaffolder.FieldMapping("ONBSZ0460_O", "pageInfo", "customPaging", true),
                new MciResponseScaffolder.FieldMapping("ONBSZ0460_O", "scrPageInfo", "scrollPagingInfo", true));

        MciResponseScaffolder.GeneratedSources generated = MciResponseScaffolder.generate(
                parsed,
                "io.shinhanlife.dat.mcc.biz.pro.dto",
                "CustomerResponse",
                "io.shinhanlife.dat.mcc.biz.pro.converter",
                "CustomerConverter",
                mappings);

        // pageInfo and scrPageInfo must be preserved without renaming
        assertTrue(generated.responseSource().contains("private PageInfo pageInfo;"));
        assertTrue(generated.responseSource().contains("private ScrPageInfo scrPageInfo;"));
        assertTrue(generated.responseSource().contains("@JsonDeserialize(using = ScrPageInfoDeserializer.class)"));
        assertTrue(generated.responseSource().contains("import io.shinhanlife.glow.db.dto.PageInfo;"));
        assertTrue(generated.responseSource().contains("import io.shinhanlife.glow.db.dto.ScrPageInfo;"));
        assertTrue(generated.responseSource().contains("import io.shinhanlife.dat.lib.paging.ScrPageInfoDeserializer;"));

        // Converter should not have unnecessary @Mapping renaming since names match
        assertFalse(generated.converterSource().contains("@Mapping(source = \"pageInfo\", target = \"customPaging\")"));
        assertFalse(generated.converterSource().contains("@Mapping(source = \"scrPageInfo\", target = \"scrollPagingInfo\")"));
    }
}
