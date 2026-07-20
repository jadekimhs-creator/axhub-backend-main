package io.shinhanlife.dap.mcc.dto;

/**
 * @package io.shinhanlife.dap.mcc.dto
 * @className WeatherRes
 * @description 기상 조회 응답 클래스
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
public record WeatherRes(
    String city,
    double temperature,
    double windSpeed,
    String reportTime,
    String summary
) {
}
