package io.shinhanlife.dap.mcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * @package io.shinhanlife.dap.mcc.dto
 * @className WeatherReq
 * @description 기상 조회 요청 클래스
 * @author 김형식
 * @create 2026.07.14
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.07.14  김형식    최초생성
 * 
 * </pre>
 */
public record WeatherReq(
    @JsonProperty(required = true, value = "city")
    @JsonPropertyDescription("날씨를 조회할 도시 이름 (예: 서울, 부산, 제주)")
    String city
) {
}
