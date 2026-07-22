package io.shinhanlife.dap.mcc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * @package io.shinhanlife.dap.mcc.dto
 * @className SearchHrRes
 * @description AX HUB 시스템 처리 클래스
 * @author user
 * @create 2026.07.22
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.07.22  user    최초생성
 *
 * </pre>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchHrRes {
    private String status;
    private String message;
    // TODO: Add response fields here
}
