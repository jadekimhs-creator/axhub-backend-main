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
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성 (전문 명세 기반 파라미터 추가)
 * 
 * </pre>
 */
@Data
public class Onnba3011ReqDto {

    @JsonPropertyDescription("통합기계약반영여부 (길이: 1)")
    @JsonProperty("untgKcntRflYn")
    private String untgKcntRflYn;

    @JsonPropertyDescription("처리구분코드 (길이: 2)")
    @JsonProperty("prcsDscd")
    private String prcsDscd;

    @JsonPropertyDescription("고객식별번호 (길이: 13)")
    @JsonProperty("cstIdntNo")
    private String cstIdntNo;

    @JsonPropertyDescription("고객번호 (길이: 10)")
    @JsonProperty("cstNo")
    private String cstNo;

    @JsonPropertyDescription("주민등록번호 (길이: 13)")
    @JsonProperty("rrnNo")
    private String rrnNo;

    @JsonPropertyDescription("불량고객여부 (길이: 1)")
    @JsonProperty("badCstYn")
    private String badCstYn;

    @JsonPropertyDescription("집중심사여부 (길이: 1)")
    @JsonProperty("cnsJdgYn")
    private String cnsJdgYn;
}
