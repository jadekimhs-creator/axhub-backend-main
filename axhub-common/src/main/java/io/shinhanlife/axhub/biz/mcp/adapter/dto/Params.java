package io.shinhanlife.axhub.biz.mcp.adapter.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * @package io.shinhanlife.axhub.biz.mcp.adapter.dto
 * @className Params
 * @description AX HUB 시스템 처리 클래스
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
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Params {
    private String routingType;
    private String name;
    private String interfaceId;
    private Map<String, Object> data;
    private List<Map<String, Object>> spec;
}