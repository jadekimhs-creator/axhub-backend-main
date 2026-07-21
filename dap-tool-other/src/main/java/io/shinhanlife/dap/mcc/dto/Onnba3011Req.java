package io.shinhanlife.dap.mcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

/**
 * @package io.shinhanlife.dap.mcc.dto
 * @className Onnba3011ReqDto
 * @description 보종By가입설계한도계산조회 MCI 요청 전문 (ONNBA3011_I)
 * @author 김형식
 * @create 2026.09.01
 */
@Data
public class Onnba3011Req {

    @JsonPropertyDescription("통합기계약보험 (유형: gs, 길이: 72)")
    @JsonProperty("unfcPrbuIrcoAddu")
    private UnfcPrbuIrcoAdduDto unfcPrbuIrcoAddu;

    @JsonPropertyDescription("처리구분코드 (길이: 1)")
    @JsonProperty("dalScCd")
    private String dalScCd;

    @JsonPropertyDescription("고객청약관계 (길이: 2)")
    @JsonProperty("cstSucoRltyCd")
    private String cstSucoRltyCd;

    @JsonPropertyDescription("고객번호 (길이: 12)")
    @JsonProperty("csNo")
    private String csNo;

    @JsonPropertyDescription("주민등록번호 (길이: 50)")
    @JsonProperty("rdreNo")
    private String rdreNo;

    @JsonPropertyDescription("통합급부계산 (길이: 1)")
    @JsonProperty("unfcPvsCalReqYn")
    private String unfcPvsCalReqYn;

    @JsonPropertyDescription("한국신용정보 (길이: 1)")
    @JsonProperty("kcisPymmTnnrReq")
    private String kcisPymmTnnrReq;

    @JsonPropertyDescription("한도초과여부 (길이: 1)")
    @JsonProperty("lmovYn")
    private String lmovYn;

    @JsonPropertyDescription("일반경유승인 (길이: 1)")
    @JsonProperty("genPsthApvTrgtYn")
    private String genPsthApvTrgtYn;

    @JsonPropertyDescription("보험사한도초과 (길이: 1)")
    @JsonProperty("ircoLmovEcpbTrgtYn")
    private String ircoLmovEcpbTrgtYn;

    @JsonPropertyDescription("진단계산여부 (길이: 1)")
    @JsonProperty("digCalYn")
    private String digCalYn;

    @JsonPropertyDescription("기계약포함진단 (길이: 1)")
    @JsonProperty("prbuIciDigCalYn")
    private String prbuIciDigCalYn;

    @JsonPropertyDescription("청약심사기본Dto (유형: gs, 길이: 2532)")
    @JsonProperty("sucoIspaBasDto")
    private SucoIspaBasDto sucoIspaBasDto;

    // ----- Nested DTO Classes -----

    @Data
    public static class UnfcPrbuIrcoAdduDto {
        // 실제 필요한 하위 필드들 추가 (사진 생략부분)
    }

    @Data
    public static class SucoIspaBasDto {

        @JsonPropertyDescription("계약처리유형 (길이: 2)")
        @JsonProperty("ccnDalTypCd")
        private String ccnDalTypCd;

        @JsonPropertyDescription("신계약입력경로 (길이: 2)")
        @JsonProperty("nwcnptCursCd")
        private String nwcnptCursCd;

        @JsonPropertyDescription("개인단체계약 (길이: 2)")
        @JsonProperty("induAsctScCd")
        private String induAsctScCd;

        @JsonPropertyDescription("모집조직번호 (길이: 7)")
        @JsonProperty("cepeOgnzNo")
        private String cepeOgnzNo;

        @JsonPropertyDescription("모집자사번번호 (길이: 8)")
        @JsonProperty("cepePrafNo")
        private String cepePrafNo;

        @JsonPropertyDescription("수금조직번호 (길이: 7)")
        @JsonProperty("clmoOgnzNo")
        private String clmoOgnzNo;

        @JsonPropertyDescription("수금자사번번호 (길이: 8)")
        @JsonProperty("clmoPrafNo")
        private String clmoPrafNo;

        @JsonPropertyDescription("청약일자 (길이: 20)")
        @JsonProperty("sucoYmd")
        private String sucoYmd;

        @JsonPropertyDescription("발행일자 (길이: 20)")
        @JsonProperty("ispDt")
        private String ispDt;

        @JsonPropertyDescription("계약일자 (길이: 20)")
        @JsonProperty("contYmd")
        private String contYmd;
    }
}
