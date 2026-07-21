package io.shinhanlife.dap.other.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.shinhanlife.dap.mcc.annotation.McpParameter;

/**
 * @package io.shinhanlife.dap.other.dto
 * @className MyBizDataDto
 * @description AX HUB 시스템 처리 클래스 - 샘플 비즈니스 데이터 DTO
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyBizDataDto {

    @McpParameter(description = "고객 이름", required = true)
    private String customerName;

    @McpParameter(description = "조회 대상 일자 (YYYYMMDD)", required = true)
    private String targetDate;

    @McpParameter(description = "비고 내용")
    private String remarks;
}
